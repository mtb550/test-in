package org.testin.java.codegen.method.update;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.util.Optional;
import java.util.List;
import java.util.function.Consumer;

public class UpdateTestBase {

    /**
     * The generated method for this test case, found by the id in its @Test
     * annotation - empty when the class holds no such method.
     */
    protected @NotNull Optional<PsiMethod> findMethodByTestName(final @NotNull PsiClass pc, final @NotNull TestCaseDto tc) {
        return GeneratedMethod.forCase(pc, tc);
    }

    /**
     * The method's @Test annotation, empty on a method that has none.
     */
    protected @NotNull Optional<PsiAnnotation> getTestAnnotation(final @NotNull PsiMethod pm) {
        return GeneratedMethod.testAnnotationOf(pm);
    }

    /**
     * Sets one attribute of an annotation, through the PSI rather than by
     * editing its text.
     * <p>
     * It used to find the attribute in the rendered text and splice the new
     * value in over the old one, ending the old value at the first comma,
     * bracket or newline it met - inside a quoted string as readily as outside
     * one. A description reading "Login, then log out" or "Login (as admin)"
     * spliced the annotation at the punctuation within the quotes, and the
     * generated class stopped compiling or the reparse threw. Every case in
     * that test set stopped running, and nothing told the tester.
     * <p>
     * setDeclaredAttributeValue is the platform's own answer and it replaces or
     * adds without either branch being written here. The value is parsed from a
     * throwaway annotation rather than as an expression, because an attribute
     * can legally be an array initializer - the groups attribute is one - and
     * {@code {"a", "b"}} is not a Java expression.
     */
    protected void updateAnnotationAttribute(final @NotNull PsiElementFactory pf, final @NotNull PsiAnnotation pa, final @NotNull String attrName, final @NotNull String newValue) {
        final @NotNull PsiAnnotation parsed = pf.createAnnotationFromText("@A(v = " + newValue + ")", pa);

        final PsiAnnotationMemberValue value = parsed.findDeclaredAttributeValue("v");
        if (value == null) {
            Logger.warn("Could not read '" + newValue + "' as a value for " + attrName);
            return;
        }

        pa.setDeclaredAttributeValue(attrName, value);
    }

    /**
     * Updates one attribute of the method's @Test annotation and reformats the method.
     * The concrete update actions only differ in the attribute name and value expression.
     */
    protected void updateTestAnnotationAttribute(final @NotNull Project p, final @NotNull PsiMethod pm, final @NotNull String attrName, final @NotNull String newValue) {
        getTestAnnotation(pm).ifPresentOrElse(testAnnotation -> {
            updateAnnotationAttribute(JavaPsiFacade.getElementFactory(p), testAnnotation, attrName, newValue);
            CodeStyleManager.getInstance(p).reformat(pm);
        }, () -> Logger.warn("Update: method has no @Test annotation"));
    }

    /**
     * Takes one attribute off the method's {@code @Test} annotation, leaving
     * the rest of it alone.
     * <p>
     * A sibling of the method above rather than a value it could be passed: the
     * platform removes an attribute when it is set to nothing, and nothing is
     * not something {@code updateAnnotationAttribute} can parse out of an
     * annotation it builds to read the value from.
     */
    protected void removeTestAnnotationAttribute(final @NotNull Project p, final @NotNull PsiMethod pm, final @NotNull String attrName) {
        getTestAnnotation(pm).ifPresentOrElse(testAnnotation -> {
            testAnnotation.setDeclaredAttributeValue(attrName, null);
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
     * The same, where a case with no generated method is the normal state
     * rather than news.
     * <p>
     * Removing a method that is not there needs no saying, and neither does
     * renumbering one. The order rewrite sweeps every case in the set each time
     * one is created or dragged, so a balloon per case without code turned
     * creating a test case into a balloon about the case just created - and a
     * set nobody has generated code for into one balloon per case it holds.
     */
    protected void applyIfGenerated(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String title, final @NotNull Consumer<PsiMethod> updater) {
        applyToMethod(p, tc, title, updater, detail -> Logger.debug("No generated method for '" + tc.getDescription() + "': " + detail));
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
