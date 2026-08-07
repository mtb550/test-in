package org.testin.generateJavaCode.method;

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
import org.testin.enums.Group;
import org.testin.generateJavaCode.GeneratorAction;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Tools;

import java.io.IOException;
import java.util.List;

public class CreateTestMethod implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;

        final List<String> fqcn = Services.getInstance(p, Tools.class).buildFqcnMethod(tc);
        final String methodName = fqcn.getLast();
        final String path = String.join(".", fqcn.subList(0, fqcn.size() - 1));
        final List<String> packageList = fqcn.subList(0, fqcn.size() - 2);
        final String className = fqcn.get(fqcn.size() - 2);

        Logger.info("Creating Test Case for: " + fqcn);

        ApplicationManager.getApplication().invokeLater(() ->
                WriteCommandAction.runWriteCommandAction(p, "Create Test Method", null, () ->
                        createMethod(p, path, packageList, className, methodName, tc)
                ));
    }

    public void executeSync(final @NotNull Project p, final @Nullable TestCaseDto tc, final @NotNull List<String> fqcn) {
        if (fqcn.size() < 2) {
            Logger.error("FQCN list is too short to generate a method.");
            return;
        }

        final String methodName = fqcn.getLast();
        final String path = String.join(".", fqcn.subList(0, fqcn.size() - 1));
        final List<String> packageList = fqcn.subList(0, fqcn.size() - 2);
        final String className = fqcn.get(fqcn.size() - 2);

        Logger.info("Creating Test Case (sync) for: " + fqcn);

        try {
            WriteCommandAction.runWriteCommandAction(p, "Create Test Method", null, () ->
                    createMethod(p, path, packageList, className, methodName, tc)
            );
        } catch (final Exception ex) {
            Logger.error("Failed to inject Java method '" + methodName + "': " + ex.getMessage());
        }
    }

    private void createMethod(final @NotNull Project p, final @NotNull String path, final @NotNull List<String> packageList, final @NotNull String className, final @NotNull String methodName, final @Nullable TestCaseDto tc) {
        try {
            final PsiClass targetClass = findOrCreateClass(p, path, packageList, className);
            if (targetClass != null) {
                injectMethod(p, targetClass, methodName, tc);
            } else
                retryInjectPhysically(p, packageList, className, methodName, tc);

        } catch (final Exception ex) {
            Logger.error("Failed to inject Java method: " + ex.getMessage());
        }
    }

    @Nullable
    private PsiClass findOrCreateClass(final @NotNull Project p, final @NotNull String path, final @NotNull List<String> packageList, final @NotNull String className) {
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(p);
        GlobalSearchScope scope = GlobalSearchScope.projectScope(p);

        PsiClass targetClass = psiFacade.findClass(path, scope);
        if (targetClass != null) return targetClass;

        try {
            VirtualFile sourceRoot = Services.getInstance(p, Tools.class).getTestSourceRoot(p);
            if (sourceRoot != null) {
                VirtualFile packageDir = VfsUtil.createDirectoryIfMissing(sourceRoot, String.join("/", packageList).toLowerCase());
                if (packageDir != null) {
                    String fileName = className + ".java";
                    VirtualFile javaFile = packageDir.findChild(fileName);
                    if (javaFile == null) {
                        javaFile = packageDir.createChildData(this, fileName);
                        String packageName = String.join(".", packageList);
                        String fileContent = packageName.isEmpty()
                                ? "public class " + className + " {\n\n}\n"
                                : "package " + packageName + ";\n\npublic class " + className + " {\n\n}\n";
                        VfsUtil.saveText(javaFile, fileContent);
                        javaFile.refresh(false, false);
                    }
                }
            }

        } catch (final IOException ex) {
            Logger.error("Failed to create class file for '" + className + "': " + ex.getMessage());
        }

        PsiDocumentManager.getInstance(p).commitAllDocuments();
        return psiFacade.findClass(path, scope);
    }

    private void retryInjectPhysically(final @NotNull Project p, final List<String> packageList, final String className, final String methodName, final TestCaseDto tc) {
        try {
            VirtualFile sourceRoot = Services.getInstance(p, Tools.class).getTestSourceRoot(p);
            if (sourceRoot == null) {
                Logger.error("retryInjectPhysically: sourceRoot is null, cannot inject method '" + methodName + "'");
                return;
            }

            String relativePath = String.join("/", packageList).toLowerCase() + "/" + className + ".java";
            VirtualFile javaFile = sourceRoot.findFileByRelativePath(relativePath);

            if (javaFile != null) {
                PsiFile psiFile = PsiManager.getInstance(p).findFile(javaFile);
                if (psiFile instanceof PsiJavaFile javaPsiFile) {
                    PsiClass[] classes = javaPsiFile.getClasses();

                    if (classes.length > 0) {
                        injectMethod(p, classes[0], methodName, tc);

                    } else
                        Logger.error("retryInjectPhysically: no classes found in " + className + ".java for method '" + methodName + "'");

                } else
                    Logger.error("retryInjectPhysically: file " + className + ".java is not a valid Java file for method '" + methodName + "'");

            } else {
                Logger.error("retryInjectPhysically: file not found at " + relativePath + " for method '" + methodName + "'");
            }
        } catch (final Exception ex) {
            Logger.error("retryInjectPhysically failed for method '" + methodName + "': " + ex.getMessage());
        }
    }

    private void injectMethod(final @NotNull Project p, final PsiClass targetClass, final String methodName, final TestCaseDto tc) {
        try {
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(p);
            PsiFile file = targetClass.getContainingFile();

            if (file instanceof PsiJavaFile javaFile) {
                PsiImportList importList = javaFile.getImportList();
                if (importList != null && importList.findSingleClassImportStatement("org.testng.annotations.Test") == null) {
                    PsiClass testClass = JavaPsiFacade.getInstance(p).findClass("org.testng.annotations.Test", GlobalSearchScope.allScope(p));
                    if (testClass != null) {
                        importList.add(factory.createImportStatement(testClass));
                    }
                }
            }

            boolean methodExists = false;
            for (PsiMethod m : targetClass.getMethods()) {
                if (m.getName().equals(methodName)) {
                    methodExists = true;
                    break;
                }
            }

            if (!methodExists) {
                StringBuilder attributes = new StringBuilder();

                if (!tc.getGroup().isEmpty()) {
                    List<String> activeGroups = tc.getGroup().stream()
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

                String annotationText = String.format("@Test(description = \"%s\", testName = \"%s\"%s)",
                        tc.getDescription().replace("\"", "\\\""),
                        tc.getId(),
                        attributes);

                String methodText = annotationText + "\npublic void " + methodName + "() {\n    // TODO: Auto-generated test steps for " + methodName + "\n}";

                PsiMethod newMethod = factory.createMethodFromText(methodText, targetClass);
                PsiElement addedElement = targetClass.add(newMethod);
                CodeStyleManager.getInstance(p).reformat(addedElement);

                Logger.info("Injected method: " + methodName + " with Priority: " + tc.getPriority().getName());
            } else
                Logger.info("Method already exists: " + methodName);

        } catch (final Exception ex) {
            Logger.error("injectMethod failed for '" + methodName + "': " + ex.getMessage());
        }
    }
}