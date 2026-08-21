package org.testin.codegen.method;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.codegen.JavaSourceRoot;
import org.testin.logger.Logger;
import org.testin.model.Group;
import org.testin.model.dto.TestCaseDto;

import java.io.IOException;
import java.util.Optional;
import java.util.List;

public class CreateTestMethod implements GenAction {

    private static final String TESTNG_TEST = "org.testng.annotations.Test";

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

        final List<String> fqcn = Fqcn.ofMethod(tc);

        parse(fqcn).ifPresentOrElse(
                target -> {
                    Logger.info("Creating Test Case for: " + fqcn);
                    WriteCommandAction.runWriteCommandAction(p, "Create Test Method", null,
                            () -> createMethod(p, target, tc));
                },
                () -> Logger.error("FQCN list is too short to generate a method: " + fqcn));
    }

    private void createMethod(final @NotNull Project p, final @NotNull Target target, final @NotNull TestCaseDto tc) {
        final List<String> packageList = target.packageList();
        final String className = target.className();
        final String methodName = target.methodName();

        try {
            findOrCreateClass(p, target.path(), packageList, className).ifPresentOrElse(
                    targetClass -> injectMethod(p, targetClass, methodName, tc),
                    () -> retryInjectPhysically(p, packageList, className, methodName, tc));

        } catch (final Exception ex) {
            Logger.error("Failed to inject Java method: " + ex.getMessage());
        }
    }

    /**
     * The class the method goes in, written out first if it is not there yet.
     * Empty when it could not be found or created, which is what sends the
     * caller down the physical-injection path.
     */
    private @NotNull Optional<PsiClass> findOrCreateClass(final @NotNull Project p, final @NotNull String path,
                                                          final @NotNull List<String> packageList,
                                                          final @NotNull String className) {
        final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(p);
        final GlobalSearchScope scope = GlobalSearchScope.projectScope(p);

        final Optional<PsiClass> existing = Optional.ofNullable(psiFacade.findClass(path, scope));
        if (existing.isPresent()) return existing;

        try {
            JavaSourceRoot.inRootOrWarn(p, root -> JavaSourceRoot.classFile(root, packageList, className));

        } catch (final IOException ex) {
            Logger.error("Failed to create class file for '" + className + "': " + ex.getMessage());
        }

        PsiDocumentManager.getInstance(p).commitAllDocuments();
        return Optional.ofNullable(psiFacade.findClass(path, scope));
    }

    private void retryInjectPhysically(final @NotNull Project p, final @NotNull List<String> packageList,
                                       final @NotNull String className, final @NotNull String methodName,
                                       final @NotNull TestCaseDto tc) {
        JavaSourceRoot.find(p).ifPresentOrElse(
                sourceRoot -> injectIntoFile(p, sourceRoot, packageList, className, methodName, tc),
                () -> Logger.error("retryInjectPhysically: no Java test source root, cannot inject method '"
                        + methodName + "'"));
    }

    /**
     * The fallback path: the class file is on disk but the PSI did not give us
     * the class, so it is read back and the method injected into it.
     */
    private void injectIntoFile(final @NotNull Project p, final @NotNull VirtualFile sourceRoot,
                                final @NotNull List<String> packageList, final @NotNull String className,
                                final @NotNull String methodName, final @NotNull TestCaseDto tc) {
        try {
            final String relativePath = String.join("/", packageList) + "/" + className + ".java";
            final VirtualFile javaFile = sourceRoot.findFileByRelativePath(relativePath);
            if (javaFile == null) {
                Logger.error("retryInjectPhysically: file not found at " + relativePath + " for method '" + methodName + "'");
                return;
            }

            final PsiFile psiFile = PsiManager.getInstance(p).findFile(javaFile);
            if (!(psiFile instanceof PsiJavaFile javaPsiFile)) {
                Logger.error("retryInjectPhysically: file " + className + ".java is not a valid Java file for method '" + methodName + "'");
                return;
            }

            final PsiClass[] classes = javaPsiFile.getClasses();
            if (classes.length == 0) {
                Logger.error("retryInjectPhysically: no classes found in " + className + ".java for method '" + methodName + "'");
                return;
            }

            injectMethod(p, classes[0], methodName, tc);

        } catch (final Exception ex) {
            Logger.error("retryInjectPhysically failed for method '" + methodName + "': " + ex.getMessage());
        }
    }

    /**
     * Puts the TestNG @Test import in the file, when it is not there already.
     * <p>
     * Both of the platform's empty answers mean the same thing here - a file
     * with no import list of its own, and a TestNG that is not on the classpath
     * - so neither is asked about separately.
     */
    private void addTestImport(final @NotNull Project p, final @NotNull PsiJavaFile javaFile,
                               final @NotNull PsiElementFactory factory) {
        Optional.ofNullable(javaFile.getImportList())
                .filter(imports -> imports.findSingleClassImportStatement(TESTNG_TEST) == null)
                .ifPresent(imports -> Optional
                        .ofNullable(JavaPsiFacade.getInstance(p).findClass(TESTNG_TEST, GlobalSearchScope.allScope(p)))
                        .ifPresent(testClass -> imports.add(factory.createImportStatement(testClass))));
    }

    private void injectMethod(final @NotNull Project p, final @NotNull PsiClass targetClass,
                              final @NotNull String methodName, final @NotNull TestCaseDto tc) {
        try {
            final PsiElementFactory factory = JavaPsiFacade.getElementFactory(p);
            final PsiFile file = targetClass.getContainingFile();

            if (file instanceof PsiJavaFile javaFile) addTestImport(p, javaFile, factory);

            for (final PsiMethod m : targetClass.getMethods()) {
                if (m.getName().equals(methodName)) {
                    Logger.info("Method already exists: " + methodName);
                    return;
                }
            }

            final StringBuilder attributes = new StringBuilder();

            if (!tc.getGroup().isEmpty()) {
                final List<String> activeGroups = tc.getGroup().stream()
                        .filter(g -> g != Group.UNASSIGNED)
                        .map(g -> "\"" + g.getName() + "\"")
                        .toList();

                if (!activeGroups.isEmpty()) {
                    attributes.append(", groups = {")
                            .append(String.join(", ", activeGroups))
                            .append("}");
                }
            }

            attributes.append(", priority = ").append(tc.getPriority().getValue());

            final String annotationText = String.format("@Test(description = \"%s\", testName = \"%s\"%s)",
                    tc.getDescription().replace("\"", "\\\""),
                    tc.getId(),
                    attributes);

            final String methodText = annotationText + "\npublic void " + methodName + "() {\n    // TODO: Auto-generated test steps for " + methodName + "\n}";

            final PsiMethod newMethod = factory.createMethodFromText(methodText, targetClass);
            final PsiElement addedElement = targetClass.add(newMethod);
            CodeStyleManager.getInstance(p).reformat(addedElement);

            Logger.info("Injected method: " + methodName + " with Priority: " + tc.getPriority().getName());

        } catch (final Exception ex) {
            Logger.error("injectMethod failed for '" + methodName + "': " + ex.getMessage());
        }
    }

    /**
     * The pieces of a fully qualified method name: everything before the method
     * for the file path, the package segments, the class, and the method.
     */
    record Target(@NotNull String path, @NotNull List<String> packageList,
                  @NotNull String className, @NotNull String methodName) {
    }
}