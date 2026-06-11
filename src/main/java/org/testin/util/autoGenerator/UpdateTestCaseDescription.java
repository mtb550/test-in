package org.testin.util.autoGenerator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.Bundle;
import org.testin.util.Tools;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import java.util.ArrayList;
import java.util.List;

public class UpdateTestCaseDescription {

    public void execute(Project project, List<String> rawFqcn, TestCaseDto tc) {
        if (rawFqcn == null || rawFqcn.isEmpty()) return;

        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(() -> {

                    List<String> cleanedFqcn = sanitizeFqcn(rawFqcn);
                    if (cleanedFqcn.isEmpty()) return;

                    List<String> packageList = new ArrayList<>(cleanedFqcn);
                    String baseClassName = packageList.removeLast();
                    String expectedClassName = Services.getInstance(project, Tools.class).toPascalCase(baseClassName);

                    if (expectedClassName.toLowerCase().endsWith("test")) {
                        if (expectedClassName.endsWith("test")) {
                            expectedClassName = expectedClassName.substring(0, expectedClassName.length() - 4) + "Test";
                        }
                    } else {
                        expectedClassName += "Test";
                    }

                    String fqcnString = String.join(".", packageList).toLowerCase() + "." + expectedClassName;

                    PsiClass targetClass = JavaPsiFacade.getInstance(project).findClass(fqcnString, GlobalSearchScope.projectScope(project));

                    if (targetClass != null) {

                        ApplicationManager.getApplication().invokeLater(() -> {
                            WriteCommandAction.runWriteCommandAction(project, "Update Test Case Description", "Testin Plugin", () -> {

                                PsiMethod targetMethod = null;
                                PsiAnnotation targetAnnotation = null;

                                for (PsiMethod method : targetClass.getMethods()) {
                                    PsiAnnotation annotation = method.getAnnotation("org.testng.annotations.Test");
                                    if (annotation != null) {
                                        PsiAnnotationMemberValue testNameAttr = annotation.findAttributeValue("testName");
                                        if (testNameAttr != null && testNameAttr.getText().contains(tc.getId().toString())) {
                                            targetMethod = method;
                                            targetAnnotation = annotation;
                                            break;
                                        }
                                    }
                                }

                                if (targetMethod != null) {
                                    PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

                                    PsiAnnotationMemberValue newDesc = factory.createExpressionFromText("\"" + tc.getDescription() + "\"", null);
                                    targetAnnotation.setDeclaredAttributeValue("description", newDesc);

                                    String newMethodName = Services.getInstance(project, Tools.class).toCamelCase(tc.getDescription());
                                    if (!targetMethod.getName().equals(newMethodName)) {
                                        targetMethod.setName(newMethodName);
                                    }

                                    CodeStyleManager.getInstance(project).reformat(targetAnnotation);
                                    Log.info("Successfully updated description and method name for: " + tc.getId());
                                } else {
                                    Messages.showWarningDialog("Could not find the Java method for this Test Case ID.", "Method Not Found");
                                }
                            });
                        });
                    }
                })
        );
    }

    // todo, it is duplicated now, move this method to Tools class
    private List<String> sanitizeFqcn(List<String> rawFqcn) {
        List<String> sanitized = new ArrayList<>();
        boolean startAdding = false;
        for (String part : rawFqcn) {
            if (startAdding) {
                if (!part.equalsIgnoreCase(DirectoryType.TCD.getDisplayedName())) {
                    sanitized.add(part.replace(" ", "").toLowerCase());
                }
            }
            if (part.equalsIgnoreCase(Bundle.getPluginName())) startAdding = true;
        }
        return sanitized;
    }
}