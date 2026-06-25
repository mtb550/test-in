package org.testin.util.autoGenerator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.Group;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.logger.Log;

import java.util.List;

public class UpdateTestMethod implements GeneratorAction {

    @Setter
    private GeneratorType changeType;

    @Override
    public void execute(final @NotNull Project project, final @Nullable TestCaseDto tc, final @NotNull List<String> fqcn) {
        Log.trace("execute() called — tc=" + (tc != null ? tc.getId() : null) + ", fqcn=" + fqcn + ", changeType=" + changeType);

        if (tc == null || fqcn.size() < 2) {
            Log.warn("UpdateTestMethod: missing test case or FQCN");
            return;
        }

        final String methodName = fqcn.getLast();
        final String path = String.join(".", fqcn.subList(0, fqcn.size() - 1));

        Log.info("Updating test method: " + methodName + " in " + path + " type=" + changeType);

        ApplicationManager.getApplication().invokeLater(() ->
                WriteCommandAction.runWriteCommandAction(project, "Update Test Method", null, () -> {
                    try {
                        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
                        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);

                        PsiClass targetClass = psiFacade.findClass(path, scope);
                        if (targetClass == null) {
                            Log.warn("UpdateTestMethod: class not found: " + path);
                            return;
                        }

                        final PsiMethod targetMethod = findMethodByTestName(targetClass, tc);

                        if (targetMethod == null) {
                            Log.warn("UpdateTestMethod: no method found with testName=" + tc.getId());
                            return;
                        }

                        Log.trace("Found method: " + targetMethod.getName() + " by testName=" + tc.getId());

                        if (changeType != null) {
                            switch (changeType) {
                                case UPDATE_TEST_CASE_DESCRIPTION:
                                    updateDescription(project, targetMethod, tc);
                                    break;
                                case UPDATE_TEST_CASE_GROUP:
                                    updateGroup(project, targetMethod, tc);
                                    break;
                                case UPDATE_TEST_CASE_PRIORITY:
                                    updatePriority(project, targetMethod, tc);
                                    break;

                                case UPDATE_TEST_CASE_EXPECTED_RESULT:
                                case UPDATE_TEST_CASE_MODULE:
                                case UPDATE_TEST_CASE_TEST_DATA:
                                case UPDATE_TEST_CASE_PRE_CONDITIONS:
                                case UPDATE_TEST_CASE_STEPS:
                                    Log.info("UpdateTestMethod: " + changeType + " is data-only (no @Test annotation change)");
                                    break;
                                default:
                                    Log.warn("UpdateTestMethod: unsupported change type: " + changeType);
                                    break;
                            }
                        }

                        Log.info("Updated test method: " + methodName);
                    } catch (Exception ex) {
                        Log.error("Failed to update test method: " + ex.getMessage());
                    }
                }));
    }

    private void updateDescription(final @NotNull Project project, final @NotNull PsiMethod method, final @NotNull TestCaseDto tc) {
        final PsiAnnotation testAnnotation = getTestAnnotation(method);
        if (testAnnotation == null) return;

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        String newValue = "\"" + tc.getDescription().replace("\"", "\\\"") + "\"";

        updateAnnotationAttribute(factory, testAnnotation, "description", newValue);
        CodeStyleManager.getInstance(project).reformat(method);
    }

    private void updateGroup(final @NotNull Project project, final @NotNull PsiMethod method, final @NotNull TestCaseDto tc) {
        final PsiAnnotation testAnnotation = getTestAnnotation(method);
        if (testAnnotation == null) return;

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

        List<String> activeGroups = tc.getGroup().stream()
                .filter(g -> g != Group.UNASSIGNED)
                .map(g -> "\"" + g.getName() + "\"")
                .toList();

        String newValue;
        if (activeGroups.isEmpty()) {
            newValue = "{}";
        } else {
            newValue = "{" + String.join(", ", activeGroups) + "}";
        }

        updateAnnotationAttribute(factory, testAnnotation, "groups", newValue);
        CodeStyleManager.getInstance(project).reformat(method);
    }

    private void updatePriority(final @NotNull Project project, final @NotNull PsiMethod method, final @NotNull TestCaseDto tc) {
        final PsiAnnotation testAnnotation = getTestAnnotation(method);
        if (testAnnotation == null) return;

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        String newValue = String.valueOf(tc.getPriority().getValue());

        updateAnnotationAttribute(factory, testAnnotation, "priority", newValue);
        CodeStyleManager.getInstance(project).reformat(method);
    }

    @Nullable
    private PsiMethod findMethodByTestName(final @NotNull PsiClass targetClass, final @NotNull TestCaseDto tc) {
        final String targetId = tc.getId().toString();
        for (final PsiMethod m : targetClass.getMethods()) {
            final PsiAnnotation annotation = m.getModifierList().findAnnotation("org.testng.annotations.Test");
            if (annotation != null) {

                final String annText = annotation.getText();
                if (annText != null && annText.contains("testName") && annText.contains(targetId)) {
                    return m;
                }
            }
        }
        return null;
    }

    @Nullable
    private PsiAnnotation getTestAnnotation(final @NotNull PsiMethod method) {
        final PsiModifierList modifierList = method.getModifierList();
        final PsiAnnotation annotation = modifierList.findAnnotation("org.testng.annotations.Test");
        if (annotation == null) {
            Log.warn("UpdateTestMethod: method has no @Test annotation");
        }
        return annotation;
    }

    private void updateAnnotationAttribute(final @NotNull PsiElementFactory factory, final @NotNull PsiAnnotation annotation, final @NotNull String attrName, final @NotNull String newValue) {
        String annotationText = annotation.getText();

        String attrPattern = attrName + " = ";
        int attrStart = annotationText.indexOf(attrPattern);

        if (attrStart >= 0) {
            int valueStart = attrStart + attrPattern.length();
            int valueEnd = findValueEnd(annotationText, valueStart);
            String newAnnotationText = annotationText.substring(0, valueStart) + newValue +
                    annotationText.substring(valueEnd);
            PsiAnnotation newAnnotation = factory.createAnnotationFromText(newAnnotationText, null);
            annotation.replace(newAnnotation);
        } else {

            int insertPos = annotationText.lastIndexOf(')');
            if (insertPos > 0) {
                String before = annotationText.substring(0, insertPos);
                String after = annotationText.substring(insertPos);
                String separator = before.contains("=") ? ", " : "";
                String newAnnotationText = before + separator + attrName + " = " + newValue + after;
                PsiAnnotation newAnnotation = factory.createAnnotationFromText(newAnnotationText, null);
                annotation.replace(newAnnotation);
            }
        }
    }

    private int findValueEnd(final @NotNull String text, final int start) {
        if (start >= text.length()) return start;
        char first = text.charAt(start);
        if (first == '{' || first == '[') {

            int depth = 1;
            for (int i = start + 1; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') {
                    depth--;
                    if (depth == 0) return i + 1;
                }
            }
            return text.length();
        }

        int end = start;
        while (end < text.length()) {
            char c = text.charAt(end);
            if (c == ',' || c == ')' || c == '\n') break;
            end++;
        }
        return end;
    }
}
