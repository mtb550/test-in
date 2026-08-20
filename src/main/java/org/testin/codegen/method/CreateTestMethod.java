package org.testin.codegen.method;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;

        final List<String> fqcn = Fqcn.ofMethod(tc);

        parse(fqcn).ifPresentOrElse(target -> {
            Logger.info("Creating Test Case for: " + fqcn);

            ApplicationManager.getApplication().invokeLater(() ->
                    WriteCommandAction.runWriteCommandAction(p, "Create Test Method", null, () ->
                            createMethod(p, target, tc)
                    ));
        }, () -> Logger.error("FQCN list is too short to generate a method: " + fqcn));
    }

    public void executeSync(final @NotNull Project p, final @Nullable TestCaseDto tc, final @NotNull List<String> fqcn) {
        final Optional<Target> parsed = parse(fqcn);
        if (parsed.isEmpty()) {
            Logger.error("FQCN list is too short to generate a method: " + fqcn);
            return;
        }
        final Target target = parsed.get();

        Logger.info("Creating Test Case (sync) for: " + fqcn);

        try {
            WriteCommandAction.runWriteCommandAction(p, "Create Test Method", null, () ->
                    createMethod(p, target, tc)
            );
        } catch (final Exception ex) {
            Logger.error("Failed to inject Java method '" + target.methodName() + "': " + ex.getMessage());
        }
    }

    private void createMethod(final @NotNull Project p, final @NotNull Target target, final @Nullable TestCaseDto tc) {
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

        final PsiClass existing = psiFacade.findClass(path, scope);
        if (existing != null) return Optional.of(existing);

        try {
            JavaSourceRoot.inRootOrWarn(p, sourceRoot -> writeEmptyClass(sourceRoot, packageList, className));

        } catch (final IOException ex) {
            Logger.error("Failed to create class file for '" + className + "': " + ex.getMessage());
        }

        PsiDocumentManager.getInstance(p).commitAllDocuments();
        return Optional.ofNullable(psiFacade.findClass(path, scope));
    }

    /**
     * Writes the class file a generated method needs, when it is not there yet.
     * <p>
     * Package segments are camelCase (see NameSanitizer.packageName); lowercasing
     * the directory here would disagree with the emitted package declaration and
     * with CreateJavaClass, so findClass could never resolve the class.
     */
    private void writeEmptyClass(final @NotNull VirtualFile sourceRoot, final @NotNull List<String> packageList,
                                 final @NotNull String className) throws IOException {
        final VirtualFile packageDir = VfsUtil.createDirectoryIfMissing(sourceRoot, String.join("/", packageList));
        if (packageDir == null) return;

        final String fileName = className + ".java";
        if (packageDir.findChild(fileName) != null) return;

        final VirtualFile javaFile = packageDir.createChildData(this, fileName);
        final String packageName = String.join(".", packageList);
        final String fileContent = packageName.isEmpty()
                ? "public class " + className + " {\n\n}\n"
                : "package " + packageName + ";\n\npublic class " + className + " {\n\n}\n";

        VfsUtil.saveText(javaFile, fileContent);
        javaFile.refresh(false, false);
    }

    private void retryInjectPhysically(final @NotNull Project p, final @NotNull List<String> packageList,
                                       final @NotNull String className, final @NotNull String methodName,
                                       final @Nullable TestCaseDto tc) {
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
                                final @NotNull String methodName, final @Nullable TestCaseDto tc) {
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

    private void injectMethod(final @NotNull Project p, final @NotNull PsiClass targetClass,
                              final @NotNull String methodName, final @Nullable TestCaseDto tc) {
        if (tc == null) {
            Logger.error("injectMethod: no test case data for method '" + methodName + "'");
            return;
        }

        try {
            final PsiElementFactory factory = JavaPsiFacade.getElementFactory(p);
            final PsiFile file = targetClass.getContainingFile();

            if (file instanceof PsiJavaFile javaFile) {
                final PsiImportList importList = javaFile.getImportList();
                if (importList != null && importList.findSingleClassImportStatement("org.testng.annotations.Test") == null) {
                    final PsiClass testClass = JavaPsiFacade.getInstance(p).findClass("org.testng.annotations.Test", GlobalSearchScope.allScope(p));
                    if (testClass != null) {
                        importList.add(factory.createImportStatement(testClass));
                    }
                }
            }

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