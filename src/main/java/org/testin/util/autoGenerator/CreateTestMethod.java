package org.testin.util.autoGenerator;

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
import org.testin.pojo.Config;
import org.testin.pojo.Group;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.Tools;
import org.testin.util.logger.Log;

import java.util.List;

public class CreateTestMethod implements GeneratorAction {

    public void execute(final @Nullable TestCaseDto tc, final @NotNull List<String> fqcn) {
        Log.info("Creating Test Case for: " + fqcn);

        if (fqcn.size() < 2) {
            Log.error("[ERROR] FQCN list is too short to generate a method.");
            return;
        }

        final Project project = Config.getProject();

        final String methodName = fqcn.getLast();
        final String path = String.join(".", fqcn.subList(0, fqcn.size() - 1));
        final String className = fqcn.get(fqcn.size() - 2);
        final List<String> packageList = fqcn.subList(0, fqcn.size() - 2);
        final String packageName = String.join(".", packageList);

        Log.info("Class Path: " + path);
        Log.info("MethodName: " + methodName);

        ApplicationManager.getApplication().invokeLater(() ->
                WriteCommandAction.runWriteCommandAction(project, "Create Test Method", null, () -> {
                    try {
                        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
                        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);

                        PsiClass targetClass = psiFacade.findClass(path, scope);

                        if (targetClass == null) {
                            VirtualFile sourceRoot = Tools.getInstance().getTestSourceRoot(project);
                            if (sourceRoot != null) {
                                String relativePath = String.join("/", packageList).toLowerCase();
                                VirtualFile packageDir = VfsUtil.createDirectoryIfMissing(sourceRoot, relativePath);

                                if (packageDir != null) {
                                    String fileName = className + ".java";
                                    VirtualFile javaFile = packageDir.findChild(fileName);

                                    if (javaFile == null) {
                                        javaFile = packageDir.createChildData(this, fileName);
                                        String fileContent;
                                        if (packageName.isEmpty()) {
                                            fileContent = "public class " + className + " {\n\n}\n";
                                        } else {
                                            fileContent = "package " + packageName + ";\n\npublic class " + className + " {\n\n}\n";
                                        }
                                        VfsUtil.saveText(javaFile, fileContent);
                                        javaFile.refresh(false, false);
                                    }
                                }
                            }

                            PsiDocumentManager.getInstance(project).commitAllDocuments();
                            targetClass = psiFacade.findClass(path, scope);
                        }

                        if (targetClass != null) {
                            injectMethod(project, targetClass, methodName, tc);
                        } else {
                            retryInjectPhysically(project, packageList, className, methodName, tc);
                        }

                    } catch (Exception ex) {
                        Log.error("[ERROR] Failed to inject Java method: " + ex.getMessage());
                    }
                }));
    }

    // todo, move to tools class
    private void retryInjectPhysically(Project project, List<String> packageList, String className, String methodName, TestCaseDto tc) {
        VirtualFile sourceRoot = Tools.getInstance().getTestSourceRoot(project);
        if (sourceRoot == null) return;

        String relativePath = String.join("/", packageList).toLowerCase() + "/" + className + ".java";
        VirtualFile javaFile = sourceRoot.findFileByRelativePath(relativePath);

        if (javaFile != null) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(javaFile);
            if (psiFile instanceof PsiJavaFile javaPsiFile) {
                PsiClass[] classes = javaPsiFile.getClasses();
                if (classes.length > 0) {
                    injectMethod(project, classes[0], methodName, tc);
                }
            }
        }
    }

    // todo, move to tools class
    private void injectMethod(Project project, PsiClass targetClass, String methodName, TestCaseDto tc) {
        PsiMethod[] existingMethods = targetClass.getMethods();
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiFile file = targetClass.getContainingFile();

        if (file instanceof PsiJavaFile javaFile) {
            PsiImportList importList = javaFile.getImportList();
            if (importList != null && importList.findSingleClassImportStatement("org.testng.annotations.Test") == null) {
                PsiClass testClass = JavaPsiFacade.getInstance(project).findClass("org.testng.annotations.Test", GlobalSearchScope.allScope(project));
                if (testClass != null) {
                    importList.add(factory.createImportStatement(testClass));
                }
            }
        }

        boolean methodExists = false;
        for (PsiMethod m : existingMethods) {
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

            CodeStyleManager.getInstance(project).reformat(addedElement);

            Log.info("[TRACE] Injected method: " + methodName + " with Priority: " + tc.getPriority().getName());
        } else {
            Log.info("[WARNING] Method already exists: " + methodName);
        }
    }
}