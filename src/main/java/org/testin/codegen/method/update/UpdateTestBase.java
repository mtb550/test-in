package org.testin.codegen.method.update;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Tools;

import java.util.List;
import java.util.function.Consumer;

public class UpdateTestBase {

    protected @Nullable PsiMethod findMethodByTestName(final @NotNull PsiClass pc, final @NotNull TestCaseDto tc) {
        final String targetId = tc.getId().toString();
        for (final PsiMethod m : pc.getMethods()) {
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

    protected @Nullable PsiAnnotation getTestAnnotation(final @NotNull PsiMethod pm) {
        final PsiModifierList modifierList = pm.getModifierList();
        final PsiAnnotation annotation = modifierList.findAnnotation("org.testng.annotations.Test");
        if (annotation == null) {
            Logger.warn("Update: method has no @Test annotation");
        }
        return annotation;
    }

    protected void updateAnnotationAttribute(final @NotNull PsiElementFactory pf, final @NotNull PsiAnnotation pa,
                                             final @NotNull String attrName, final @NotNull String newValue) {
        final String annotationText = pa.getText();

        final String attrPattern = attrName + " = ";
        final int attrStart = annotationText.indexOf(attrPattern);

        if (attrStart >= 0) {
            final int valueStart = attrStart + attrPattern.length();
            final int valueEnd = findValueEnd(annotationText, valueStart);
            final String newAnnotationText = annotationText.substring(0, valueStart) + newValue +
                    annotationText.substring(valueEnd);
            pa.replace(pf.createAnnotationFromText(newAnnotationText, null));
            return;
        }

        final int insertPos = annotationText.lastIndexOf(')');
        if (insertPos <= 0) return;

        final String before = annotationText.substring(0, insertPos);
        final String after = annotationText.substring(insertPos);
        final String separator = before.contains("=") ? ", " : "";
        pa.replace(pf.createAnnotationFromText(before + separator + attrName + " = " + newValue + after, null));
    }

    protected int findValueEnd(final @NotNull String s, final int start) {
        if (start >= s.length()) return start;

        final char first = s.charAt(start);
        if (first == '{' || first == '[') {

            int depth = 1;
            for (int i = start + 1; i < s.length(); i++) {
                final char c = s.charAt(i);
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') {
                    depth--;
                    if (depth == 0) return i + 1;
                }
            }
            return s.length();
        }

        int end = start;
        while (end < s.length()) {
            final char c = s.charAt(end);
            if (c == ',' || c == ')' || c == '\n') break;
            end++;
        }
        return end;
    }

    /**
     * Updates one attribute of the method's @Test annotation and reformats the method.
     * The concrete update actions only differ in the attribute name and value expression.
     */
    protected void updateTestAnnotationAttribute(final @NotNull Project p, final @NotNull PsiMethod pm,
                                                 final @NotNull String attrName, final @NotNull String newValue) {
        final PsiAnnotation testAnnotation = getTestAnnotation(pm);
        if (testAnnotation == null) return;

        updateAnnotationAttribute(JavaPsiFacade.getElementFactory(p), testAnnotation, attrName, newValue);
        com.intellij.psi.codeStyle.CodeStyleManager.getInstance(p).reformat(pm);
    }

    // Shared boilerplate for all update actions: resolve the FQCN, locate the target class and
    // its @Test method by testName, then apply the specific update inside a write command action.
    protected void applyUpdate(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String title,
                               final @NotNull Consumer<PsiMethod> updater) {
        final List<String> fqcn = Services.getInstance(p, Tools.class).buildFqcnMethod(tc);
        if (fqcn.size() < 2) return;
        final String path = String.join(".", fqcn.subList(0, fqcn.size() - 1));

        ApplicationManager.getApplication().invokeLater(() ->
                WriteCommandAction.runWriteCommandAction(p, title, null, () -> {
                    final PsiClass targetClass = JavaPsiFacade.getInstance(p).findClass(path, GlobalSearchScope.projectScope(p));
                    if (targetClass == null) {
                        Logger.warn("Update: class not found: " + path);
                        return;
                    }
                    final PsiMethod targetMethod = findMethodByTestName(targetClass, tc);
                    if (targetMethod == null) {
                        Logger.warn("Update: no method found with testName=" + tc.getId());
                        return;
                    }
                    updater.accept(targetMethod);
                }));
    }
}
