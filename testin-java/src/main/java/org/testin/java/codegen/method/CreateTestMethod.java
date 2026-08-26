package org.testin.java.codegen.method;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.indexer.ProjectIndexer;
import org.testin.services.Services;
import org.testin.testcase.TestCaseOrder;
import org.testin.codegen.GenAction;
import org.testin.codegen.JavaSourceRoot;
import org.testin.logger.Logger;
import org.testin.model.Group;
import org.testin.model.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CreateTestMethod implements GenAction {

    private static final @NotNull String TESTNG_TEST = "org.testng.annotations.Test";

    /**
     * Splits an FQCN list into the parts the generator needs, or null when there
     * is no class and method to split into.
     * <p>
     * Both entry points read the same four values out of the list, and only the
     * sync one used to check the length first — so a short FQCN threw
     * IndexOutOfBoundsException out of the async path. Splitting in one place is
     * what stops the two drifting apart again.
     */
    static @NotNull Optional<Target> parse(final @NotNull List<String> fqcn) {
        if (fqcn.size() < 2) return Optional.empty();

        return Optional.of(new Target(
                String.join(".", fqcn.subList(0, fqcn.size() - 1)),
                fqcn.subList(0, fqcn.size() - 2),
                fqcn.get(fqcn.size() - 2),
                fqcn.getLast()));
    }

    /**
     * Writes the method here and now, in the caller's command when there is one.
     * <p>
     * A command inside a command is the outer one, so a caller generating for a
     * whole set - an import, a copied test set - opens one command around its
     * loop and gets one write lock, one reparse of the class and one undo entry
     * instead of one of each per case. This used to hand every case to
     * {@code invokeLater}, which put each one in an event of its own and so
     * outside any command the caller had opened: fifty cases meant fifty
     * separate freezes and fifty entries in the IDE's undo (#51).
     * <p>
     * On the EDT, because a write command action is. Both callers are: a dialog
     * that just closed, and the copy's completion.
     */
    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;

        // One case is a set of one. There is no second way of writing a method
        // here: the two used to differ only in that this one added through the
        // PSI and the other wrote text, which is a difference in speed, not in
        // what ends up in the file.
        executeAll(p, List.of(tc));
    }

    /**
     * Writes the methods for these cases. The only way in - one case comes
     * through it as a set of one.
     * <p>
     * A test set is one class, and the work that belongs to the class was once
     * done per case: finding it through the stub index, and reformatting after
     * every method. Both happen once per class here, and the methods go in as
     * one edit rather than one insertion each. One sheet of 550 took 38 seconds
     * to generate before that and takes under two now (#66, 25).
     * <p>
     * Grouped by class rather than assumed to be one, because nothing stops a
     * caller handing over cases from two sets.
     * <p>
     * On the EDT, because a write command action is - and one command around
     * the whole set, so a sheet is one write lock and one undo entry rather
     * than one of each per case (#51).
     */
    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
        final @NotNull Map<String, List<TestCaseDto>> byClass = new LinkedHashMap<>();

        for (final Object item : items) {
            if (!(item instanceof TestCaseDto tc)) continue;

            final @NotNull List<String> fqcn = Fqcn.ofMethod(tc);
            parse(fqcn).ifPresentOrElse(
                    target -> byClass.computeIfAbsent(target.path(), ignored -> new ArrayList<>()).add(tc),
                    () -> Logger.error("FQCN list is too short to generate a method: " + fqcn));
        }

        WriteCommandAction.runWriteCommandAction(p, "Create Test Methods", null,
                () -> byClass.values().forEach(group -> createMethods(p, group)));
    }

    /**
     * The methods of one class. Anything the class cannot be found for falls
     * back to the per-case path, which writes the file out and reads it back.
     */
    private void createMethods(final @NotNull Project p, final @NotNull List<TestCaseDto> cases) {
        final @NotNull Optional<Target> first = parse(Fqcn.ofMethod(cases.getFirst()));
        if (first.isEmpty()) return;

        final @NotNull Target target = first.orElseThrow();
        Logger.info("Creating " + testMethods(cases.size()) + " in " + target.path());

        findOrCreateClass(p, target.path(), target.packageList(), target.className()).ifPresentOrElse(
                targetClass -> injectAsText(p, targetClass, cases),
                () -> cases.forEach(tc -> retryInjectPhysically(p, target.packageList(), target.className(),
                        Fqcn.methodNameOf(tc), tc)));
    }

    /**
     * Writes a whole set of methods into the class as one edit.
     * <p>
     * Adding them through the PSI one at a time is what made this slow: every
     * add throws away the class's member cache, the next add rebuilds it, and
     * the rebuild grows with the class - so the last method of a sheet costs
     * several times the first. Measured at 550 methods, the batches went from
     * 590ms to 2,035ms across one import for identical work.
     * <p>
     * The text of every method goes in at the closing brace in a single
     * document edit, the file is parsed once, and the inserted span is
     * formatted once. Nothing reads the class between the first method and the
     * last, so nothing has to rebuild anything.
     */
    private void injectAsText(final @NotNull Project p, final @NotNull PsiClass targetClass, final @NotNull List<TestCaseDto> cases) {
        final @NotNull PsiFile file = targetClass.getContainingFile();
        final @NotNull PsiDocumentManager documents = PsiDocumentManager.getInstance(p);
        final @NotNull Optional<Document> document = Optional.ofNullable(documents.getDocument(file));

        if (document.isEmpty()) {
            oneAtATime(p, targetClass, cases, "it has no document to edit");
            return;
        }

        // Named before anything is written: the class does not change until the
        // single edit below, so what it already holds is read once and what this
        // pass adds is remembered as it goes. That also catches a sheet listing
        // one description twice, which asking the class could not.
        final @NotNull Set<String> taken = Arrays.stream(targetClass.getMethods())
                .map(PsiMethod::getName)
                .collect(Collectors.toCollection(HashSet::new));

        final @NotNull StringBuilder methods = new StringBuilder();
        int alreadyThere = 0;
        for (final TestCaseDto tc : cases) {
            final @NotNull String methodName = Fqcn.methodNameOf(tc);
            if (!taken.add(methodName)) {
                alreadyThere++;
                continue;
            }
            methods.append('\n').append(methodText(p, methodName, tc)).append('\n');
        }

        // Counted, not narrated. Re-importing a sheet skips every method in it,
        // and a line each buried everything else the import had to say.
        if (alreadyThere > 0) {
            Logger.info(alreadyThere + " of " + testMethods(cases.size())
                    + " already in " + targetClass.getQualifiedName());
        }

        if (methods.isEmpty()) return;

        if (file instanceof PsiJavaFile javaFile) addTestImport(p, javaFile, JavaPsiFacade.getElementFactory(p));

        // The import is a PSI change, and a pending PSI change locks the
        // document against being edited as text - the platform throws "Document
        // is locked by write PSI operations" rather than letting the two ways of
        // writing the same file interleave. Writing it through first is what the
        // message asks for, and it is also why the brace is located afterwards:
        // adding an import moves everything below it.
        documents.doPostponedOperationsAndUnblockDocument(document.orElseThrow());

        final @NotNull Optional<PsiElement> closingBrace = Optional.ofNullable(targetClass.getRBrace());
        if (closingBrace.isEmpty()) {
            oneAtATime(p, targetClass, cases, "it has no closing brace to write before");
            return;
        }

        final int insertAt = closingBrace.orElseThrow().getTextRange().getStartOffset();
        document.orElseThrow().insertString(insertAt, methods);
        documents.commitDocument(document.orElseThrow());

        CodeStyleManager.getInstance(p).reformatText(file, insertAt, insertAt + methods.length());
    }

    /**
     * The way that needs neither a document nor a brace, for the classes where
     * the one edit cannot be made. Slower, and says why it is being used.
     */
    private void oneAtATime(final @NotNull Project p, final @NotNull PsiClass targetClass, final @NotNull List<TestCaseDto> cases, final @NotNull String reason) {
        Logger.warn("Writing " + cases.size() + " methods one at a time into "
                + targetClass.getQualifiedName() + ": " + reason);

        cases.forEach(tc -> injectMethod(p, targetClass, Fqcn.methodNameOf(tc), tc)
                .ifPresent(added -> CodeStyleManager.getInstance(p).reformat(added)));
    }

    /**
     * The class the method goes in, written out first if it is not there yet.
     * Empty when it could not be found or created, which is what sends the
     * caller down the physical-injection path.
     */
    private @NotNull Optional<PsiClass> findOrCreateClass(final @NotNull Project p, final @NotNull String path, final @NotNull List<String> packageList, final @NotNull String className) {
        final @NotNull JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(p);
        final @NotNull GlobalSearchScope scope = GlobalSearchScope.projectScope(p);

        final @NotNull Optional<PsiClass> existing = Optional.ofNullable(psiFacade.findClass(path, scope));
        if (existing.isPresent()) return existing;

        JavaSourceRoot.inRootOrWarn(p, "creating the class for " + className,
                root -> JavaSourceRoot.classFile(root, packageList, className));

        PsiDocumentManager.getInstance(p).commitAllDocuments();
        return Optional.ofNullable(psiFacade.findClass(path, scope));
    }

    private void retryInjectPhysically(final @NotNull Project p, final @NotNull List<String> packageList, final @NotNull String className, final @NotNull String methodName, final @NotNull TestCaseDto tc) {
        JavaSourceRoot.find(p).ifPresentOrElse(
                sourceRoot -> injectIntoFile(p, sourceRoot, packageList, className, methodName, tc),
                () -> Logger.error("retryInjectPhysically: no Java test source root, cannot inject method '"
                        + methodName + "'"));
    }

    /**
     * The fallback path: the class file is on disk but the PSI did not give us
     * the class, so it is read back and the method injected into it.
     */
    private void injectIntoFile(final @NotNull Project p, final @NotNull VirtualFile sourceRoot, final @NotNull List<String> packageList, final @NotNull String className, final @NotNull String methodName, final @NotNull TestCaseDto tc) {
        try {
            final @NotNull String relativePath = String.join("/", packageList) + "/" + className + ".java";
            final @NotNull Optional<VirtualFile> found = JavaSourceRoot.under(sourceRoot, relativePath);
            if (found.isEmpty()) {
                Logger.error("retryInjectPhysically: file not found at " + relativePath + " for method '" + methodName + "'");
                return;
            }

            // instanceof answers no for a file the PSI has not loaded and for one
            // that is not Java, which are the same thing to do about here.
            if (!(PsiManager.getInstance(p).findFile(found.orElseThrow()) instanceof PsiJavaFile javaPsiFile)) {
                Logger.error("retryInjectPhysically: file " + className + ".java is not a valid Java file for method '" + methodName + "'");
                return;
            }

            final PsiClass @NotNull[] classes = javaPsiFile.getClasses();
            if (classes.length == 0) {
                Logger.error("retryInjectPhysically: no classes found in " + className + ".java for method '" + methodName + "'");
                return;
            }

            injectMethod(p, classes[0], methodName, tc)
                    .ifPresent(added -> CodeStyleManager.getInstance(p).reformat(added));

        } catch (final Exception ex) {
            Logger.error("retryInjectPhysically failed for method '" + methodName + "': " + ex.getMessage());
        }
    }

    /**
     * Whether the file already imports TestNG's @Test. The platform answers "it
     * does not" with no import statement, and this is the one place that reads
     * that.
     */
    private static boolean alreadyImportsTest(final @NotNull PsiImportList imports) {
        return imports.findSingleClassImportStatement(TESTNG_TEST) != null;
    }

    /**
     * Puts the TestNG @Test import in the file, when it is not there already.
     * <p>
     * Both of the platform's empty answers mean the same thing here - a file
     * with no import list of its own, and a TestNG that is not on the classpath
     * - so neither is asked about separately.
     */
    private void addTestImport(final @NotNull Project p, final @NotNull PsiJavaFile javaFile, final @NotNull PsiElementFactory factory) {
        Optional.ofNullable(javaFile.getImportList())
                .filter(imports -> !alreadyImportsTest(imports))
                .ifPresent(imports -> Optional
                        .ofNullable(JavaPsiFacade.getInstance(p).findClass(TESTNG_TEST, GlobalSearchScope.allScope(p)))
                        .ifPresent(testClass -> imports.add(factory.createImportStatement(testClass))));
    }

    /**
     * A count of test methods, singular when there is one. Log lines are read
     * by people.
     */
    private static @NotNull String testMethods(final int howMany) {
        return howMany + " test method" + (howMany == 1 ? "" : "s");
    }

    /**
     * The source of one generated test method: its TestNG annotation and an
     * empty body. Written here for both ways of adding it - one at a time
     * through the PSI, and a whole set as text.
     */
    /**
     * Where this case sits in its test set, one-based, which is what its
     * generated method carries as its TestNG priority.
     * <p>
     * Asked of the set rather than read off the case, because a case's order is
     * a rank - a string with room between any two of them, so a drag rewrites
     * one file instead of renumbering the set. TestNG wants an int, and the only
     * int that means the same thing is the position that rank sorts into.
     * <p>
     * A case the index has not seen yet runs last: it is being created, and
     * putting it at the end is where a tester looks for something that has just
     * arrived.
     */
    private static int executionPriority(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        final @NotNull List<TestCaseDto> inSet = TestCaseOrder.ordered(
                Services.getInstance(p, ProjectIndexer.class).getTestCasesForTestSet(tc.getParent().getPath()));

        for (int position = 0; position < inSet.size(); position++) {
            if (inSet.get(position).getId().equals(tc.getId())) return position + 1;
        }

        return inSet.size() + 1;
    }

    private static @NotNull String methodText(final @NotNull Project p, final @NotNull String methodName, final @NotNull TestCaseDto tc) {
        final @NotNull StringBuilder attributes = new StringBuilder();

        if (!tc.getGroup().isEmpty()) {
            final @NotNull List<String> activeGroups = tc.getGroup().stream()
                    .filter(g -> g != Group.UNASSIGNED)
                    .map(g -> "\"" + g.getName() + "\"")
                    .toList();

            if (!activeGroups.isEmpty()) {
                attributes.append(", groups = {").append(String.join(", ", activeGroups)).append("}");
            }
        }

        // The case's place in its test set, not its High/Medium/Low. TestNG runs
        // methods in priority order, so this is what makes a run execute in the
        // order the tester arranged - which is what the attribute is for here.
        // The case's own priority stays a Testin field, shown in the grid and
        // the reports; it decides nothing about execution.
        attributes.append(", priority = ").append(executionPriority(p, tc));

        final @NotNull String annotation = String.format("@Test(description = \"%s\", testName = \"%s\"%s)",
                tc.getDescription().replace("\"", "\\\""),
                tc.getId(),
                attributes);

        return annotation + "\npublic void " + methodName + "() {\n    // TODO: Auto-generated test steps for "
                + methodName + "\n}";
    }

    /**
     * Adds one method through the PSI and hands back what it added, or nothing
     * when the class already had it. The caller reformats what it gets.
     * <p>
     * The slow way, kept for the two places that cannot write text: a class
     * with no document to edit, and one the PSI would not give us at all, which
     * is read back off disk instead.
     */
    private @NotNull Optional<PsiElement> injectMethod(final @NotNull Project p, final @NotNull PsiClass targetClass, final @NotNull String methodName, final @NotNull TestCaseDto tc) {
        try {
            final @NotNull PsiElementFactory factory = JavaPsiFacade.getElementFactory(p);
            final @NotNull PsiFile file = targetClass.getContainingFile();

            if (file instanceof PsiJavaFile javaFile) addTestImport(p, javaFile, factory);

            // By name, not by walking every method the class already has. A
            // generated set is one class, so the walk grew with it: importing a
            // sheet of a thousand compared half a million names on the way
            // through (#66, finding 23).
            if (targetClass.findMethodsByName(methodName, false).length > 0) {
                Logger.info("Method already exists: " + methodName);
                return Optional.empty();
            }

            final @NotNull PsiMethod newMethod = factory.createMethodFromText(methodText(p, methodName, tc), targetClass);
            final @NotNull PsiElement addedElement = targetClass.add(newMethod);

            Logger.info("Injected method: " + methodName + " with Priority: " + tc.getPriority().getName());
            return Optional.of(addedElement);

        } catch (final Exception ex) {
            Logger.error("injectMethod failed for '" + methodName + "': " + ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * The pieces of a fully qualified method name: everything before the method
     * for the file path, the package segments, the class, and the method.
     */
    record Target(@NotNull String path, @NotNull List<String> packageList, @NotNull String className, @NotNull String methodName) {
    }
}