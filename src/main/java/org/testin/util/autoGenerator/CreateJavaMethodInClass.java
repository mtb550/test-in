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
import org.testin.pojo.Group;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.Tools;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CreateJavaMethodInClass {

    public void execute(@NotNull final Project project, @NotNull final List<String> rawFqcn, @NotNull final TestCaseDto tc) {
        if (rawFqcn.isEmpty() || tc.getDescription().isEmpty()) return;

        ApplicationManager.getApplication().invokeLater(() -> WriteCommandAction.runWriteCommandAction(project, "Create Test Method", null, () -> {
            try {
                List<String> cleanedFqcn = Tools.getInstance().sanitizeFqcn(rawFqcn);
                if (cleanedFqcn.isEmpty()) return;

                String targetMethodName = Tools.getInstance().formatMethodName(tc.getDescription());
                String methodName;
                String baseClassName;
                List<String> packageList = new ArrayList<>(cleanedFqcn);

                if (packageList.size() >= 2 && (packageList.get(packageList.size() - 2).toLowerCase().endsWith("test") || packageList.getLast().equalsIgnoreCase(targetMethodName))) {
                    methodName = packageList.removeLast();
                    baseClassName = packageList.removeLast();
                } else {
                    methodName = targetMethodName;
                    baseClassName = packageList.removeLast();
                }

                String expectedClassName = Tools.getInstance().toPascalCase(baseClassName);
                if (expectedClassName.toLowerCase().endsWith("test")) {
                    if (!expectedClassName.endsWith("Test")) {
                        expectedClassName = expectedClassName.substring(0, expectedClassName.length() - 4) + "Test";
                    }
                } else {
                    expectedClassName += "Test";
                }

                String packageName = packageList.stream().map(String::toLowerCase).collect(Collectors.joining("."));
                String fqcnString = packageName.isEmpty() ? expectedClassName : packageName + "." + expectedClassName;

                JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
                GlobalSearchScope scope = GlobalSearchScope.projectScope(project);

                PsiClass targetClass = psiFacade.findClass(fqcnString, scope);

                if (targetClass == null) {
                    VirtualFile sourceRoot = Tools.getInstance().getTestSourceRoot(project);
                    if (sourceRoot != null) {
                        String relativePath = String.join("/", packageList).toLowerCase();
                        VirtualFile packageDir = VfsUtil.createDirectoryIfMissing(sourceRoot, relativePath);

                        if (packageDir != null) {
                            String fileName = expectedClassName + ".java";
                            VirtualFile javaFile = packageDir.findChild(fileName);

                            if (javaFile == null) {
                                javaFile = packageDir.createChildData(this, fileName);
                                String fileContent;
                                if (packageName.isEmpty()) {
                                    fileContent = "public class " + expectedClassName + " {\n\n}\n";
                                } else {
                                    fileContent = "package " + packageName + ";\n\npublic class " + expectedClassName + " {\n\n}\n";
                                }
                                VfsUtil.saveText(javaFile, fileContent);
                                javaFile.refresh(false, false);
                            }
                        }
                    }

                    PsiDocumentManager.getInstance(project).commitAllDocuments();
                    targetClass = psiFacade.findClass(fqcnString, scope);
                }

                if (targetClass != null) {
                    injectMethod(project, targetClass, methodName, tc);
                } else {
                    retryInjectPhysically(project, packageList, expectedClassName, methodName, tc);
                }

            } catch (Exception ex) {
                System.err.println("Failed to inject Java method: " + ex.getMessage());
            }
        }));
    }

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
                    tc.getDescription(),
                    tc.getId(),
                    attributes);

            String methodText = annotationText + "\npublic void " + methodName + "() {\n    System.out.println(\"hello world\");\n}";

            PsiMethod newMethod = factory.createMethodFromText(methodText, targetClass);
            PsiElement addedElement = targetClass.add(newMethod);

            CodeStyleManager.getInstance(project).reformat(addedElement);

            System.out.println("Injected method: " + methodName + " with Priority: " + tc.getPriority().getName());
        }
    }
}