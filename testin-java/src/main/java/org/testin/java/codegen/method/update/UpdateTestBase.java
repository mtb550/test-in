package org.testin.java.codegen.method.update;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.util.Arrays;
import java.util.Optional;
import java.util.List;
import java.util.function.Consumer;

public class UpdateTestBase {

    private static final @NotNull String TEST_ANNOTATION = "org.testng.annotations.Test";

    /**
     * The generated method for this test case, found by the id in its @Test
     * annotation - empty when the class holds no such method.
     */
    protected @NotNull Optional<PsiMethod> findMethodByTestName(final @NotNull PsiClass pc, final @NotNull TestCaseDto tc) {
        final @NotNull String targetId = tc.getId().toString();

        return Arrays.stream(pc.getMethods())
                .filter(method -> getTestAnnotation(method)
                        .map(PsiAnnotation::getText)
                        .filter(text -> text.contains("testName") && text.contains(targetId))
                        .isPresent())
                .findFirst();
    }

    /**
     * The method's @Test annotation, empty on a method that has none.
     */
    protected @NotNull Optional<PsiAnnotation> getTestAnnotation(final @NotNull PsiMethod pm) {
        return Optional.ofNullable(pm.getModifierList().findAnnotation(TEST_ANNOTATION));
    }

    protected void updateAnnotationAttribute(final @NotNull PsiElementFactory pf, final @NotNull PsiAnnotation pa, final @NotNull String attrName, final @NotNull String newValue) {
        final @NotNull String annotationText = pa.getText();

        final @NotNull String attrPattern = attrName + " = ";
        final int attrStart = annotationText.indexOf(attrPattern);

        if (attrStart >= 0) {
            final int valueStart = attrStart + attrPattern.length();
            final int valueEnd = findValueEnd(annotationText, valueStart);
            final @NotNull String newAnnotationText = annotationText.substring(0, valueStart) + newValue +
                    annotationText.substring(valueEnd);
            pa.replace(pf.createAnnotationFromText(newAnnotationText, null));
            return;
        }

        final int insertPos = annotationText.lastIndexOf(')');
        if (insertPos <= 0) return;

        final @NotNull String before = annotationText.substring(0, insertPos);
        final @NotNull String after = annotationText.substring(insertPos);
        final @NotNull String separator = before.contains("=") ? ", " : "";
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
    protected void updateTestAnnotationAttribute(final @NotNull Project p, final @NotNull PsiMethod pm, final @NotNull String attrName, final @NotNull String newValue) {
        getTestAnnotation(pm).ifPresentOrElse(testAnnotation -> {
            updateAnnotationAttribute(JavaPsiFacade.getElementFactory(p), testAnnotation, attrName, newValue);
            com.intellij.psi.codeStyle.CodeStyleManager.getInstance(p).reformat(pm);
        }, () -> Logger.warn("Update: method has no @Test annotation"));
    }

    /**
     * The edit was saved and no code changed, because there is no generated
     * method to change.
     * <p>
     * Said out loud rather than written to the log and forgotten. The tester
     * watched the case change in the editor and has no other way to learn that
     * the code did not follow - it happened five times in one log before anybody
     * noticed (#66, finding 19).
     */
    private void noCodeToUpdate(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String detail) {
        Logger.warn("Update: " + detail);

        Services.getInstance(p, Notifier.class).softShowNoGeneratedCode(p, tc.getDescription());
    }

    /**
     * Applies a change to the case's generated method, and says so when there is
     * none to change.
     */
    protected void applyUpdate(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String title, final @NotNull Consumer<PsiMethod> updater) {
        applyToMethod(p, tc, title, updater, detail -> noCodeToUpdate(p, tc, detail));
    }

    /**
     * The same, for removing the method: one that is not there needs no
     * removing, so nothing is said. A balloon reading "has no generated code
     * yet" while the tester deletes a case is a balloon about nothing.
     */
    protected void applyRemoval(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String title, final @NotNull Consumer<PsiMethod> updater) {
        applyToMethod(p, tc, title, updater, detail -> Logger.debug("Remove: " + detail + ", so nothing to remove"));
    }

    // Shared boilerplate for all update actions: resolve the FQCN, locate the target class and
    // its @Test method by testName, then apply the specific update inside a write command action.
    private void applyToMethod(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String title, final @NotNull Consumer<PsiMethod> updater, final @NotNull Consumer<String> onMissing) {
        final @NotNull List<String> fqcn = Fqcn.ofMethod(tc);
        if (fqcn.size() < 2) return;
        final @NotNull String path = String.join(".", fqcn.subList(0, fqcn.size() - 1));

        ApplicationManager.getApplication().invokeLater(() ->
                WriteCommandAction.runWriteCommandAction(p, title, null, () -> {
                    Optional.ofNullable(JavaPsiFacade.getInstance(p).findClass(path, GlobalSearchScope.projectScope(p)))
                            .ifPresentOrElse(
                                    targetClass -> findMethodByTestName(targetClass, tc).ifPresentOrElse(updater,
                                            () -> onMissing.accept("no method with testName=" + tc.getId())),
                                    () -> onMissing.accept("class not found: " + path));
                }));
    }
}
