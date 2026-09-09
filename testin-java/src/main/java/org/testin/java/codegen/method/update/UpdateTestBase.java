package org.testin.java.codegen.method.update;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenType;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.logger.Logger;
import org.testin.java.codegen.JavaLiteral;
import org.testin.model.TestCaseStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.NameSanitizer;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
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
     * UC-CODEGEN-002, Rule-CODEGEN-012, Rule-CODEGEN-040.
     * <p>
     * The description onto the method: into the annotation, and into the
     * method's name.
     * <p>
     * Here rather than in the updater that used to hold it, because the restore
     * writes the same thing and two copies of "what a description does to a
     * method" is how the two come to disagree.
     */
    protected void writeDescription(final @NotNull Project p, final @NotNull PsiMethod pm, final @NotNull TestCaseDto tc) {
        updateTestAnnotationAttribute(p, pm, "description", JavaLiteral.of(tc.getDescription()));

        // A method cannot be nameless, so a description cleared to nothing
        // leaves the method under the name it already has - the same reason
        // className keeps its fallback. The annotation still records that the
        // description is now empty, which is what the case says (#155).
        final @NotNull String newMethodName = NameSanitizer.methodName(tc.getDescription());
        if (!newMethodName.isEmpty() && !pm.getName().equals(newMethodName)) {
            pm.setName(newMethodName);
        }
    }

    /**
     * UC-CODEGEN-002, Rule-CODEGEN-015, Rule-CODEGEN-046.
     * <p>
     * The case's groups onto the method, and the whole attribute rewritten
     * because a group taken away has to go as well as one added.
     * <p>
     * The last group taken away takes the attribute with it, exactly as
     * {@link #writeEnabled} below takes {@code enabled} off a case that is no
     * longer disabled. It used to write {@code groups = {}}, so a case created
     * with no groups and a case whose groups had been cleared described the same
     * state two ways in one class - and the document says the attribute is
     * written only for a case that belongs to at least one group. TestNG accepts
     * the empty braces, so nothing failed; the file simply stopped being what
     * the document describes (#287).
     */
    protected void writeGroups(final @NotNull Project p, final @NotNull PsiMethod pm, final @NotNull TestCaseDto tc) {
        final @NotNull List<String> quoted = tc.getGroup().stream().map(g -> "\"" + g + "\"").toList();

        if (quoted.isEmpty()) removeTestAnnotationAttribute(p, pm, "groups");
        else updateTestAnnotationAttribute(p, pm, "groups", "{" + String.join(", ", quoted) + "}");
    }

    /**
     * UC-CODEGEN-002, Rule-CODEGEN-047, Rule-CODEGEN-048.
     * <p>
     * Disabled writes the attribute; anything else takes it off rather than
     * writing true, so a case that was once disabled does not carry a word that
     * says nothing.
     */
    protected void writeEnabled(final @NotNull Project p, final @NotNull PsiMethod pm, final @NotNull TestCaseDto tc) {
        if (tc.getStatus() == TestCaseStatus.DISABLED) updateTestAnnotationAttribute(p, pm, "enabled", "false");
        else removeTestAnnotationAttribute(p, pm, "enabled");
    }

    /**
     * The class a case generates into, resolved from its own path.
     */
    protected static @NotNull Optional<PsiClass> classOf(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        final @NotNull List<String> fqcn = Fqcn.ofMethod(tc);
        if (fqcn.size() < 2) return Optional.empty();

        final @NotNull String path = String.join(".", fqcn.subList(0, fqcn.size() - 1));

        return Optional.ofNullable(JavaPsiFacade.getInstance(p).findClass(path, GlobalSearchScope.projectScope(p)));
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

        Services.getInstance(p, Notifier.class).softRefuse(p, Refused.NO_GENERATED_CODE, tc.getDescription());
    }

    /**
     * Applies a change to the case's generated method, and says so when there is
     * none to change.
     */
    protected void applyUpdate(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String title, final @NotNull Consumer<PsiMethod> updater) {
        applyToMethod(p, tc, title, updater, detail -> noCodeToUpdate(p, tc, detail));
    }

    /**
     * UC-CODEGEN-003, Rule-CODEGEN-019.
     * <p>
     * The same, where a case with no generated method should be given one.
     * <p>
     * A description is what names a method, so a case saved without one has no
     * method written for it at all. Filling the description in later is
     * therefore not an update to make - it is the first thing that makes the
     * method nameable - and the code follows the case (#155).
     * <p>
     * It replaces a balloon that told the tester there was no generated code and
     * left them to do something about it. Writing the method is what they would
     * have done.
     */
    protected void applyOrCreate(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String title, final @NotNull Consumer<PsiMethod> updater) {
        applyToMethod(p, tc, title, updater, detail -> {
            Logger.info("Writing the method for '" + tc.getDescription() + "' now that it has a name: " + detail);
            GenType.CREATE_TEST_CASE.getAction().execute(p, tc);
        });
    }

    /**
     * UC-CODEGEN-011, Rule-CODEGEN-044.
     * <p>
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

        final @NotNull Runnable inCommand = () ->
                WriteCommandAction.runWriteCommandAction(p, title, null, () ->
                        Optional.ofNullable(JavaPsiFacade.getInstance(p).findClass(path, GlobalSearchScope.projectScope(p)))
                                .ifPresentOrElse(
                                        targetClass -> findMethodByTestName(targetClass, tc).ifPresentOrElse(updater,
                                                () -> onMissing.accept("no method with testName=" + tc.getId())),
                                        () -> onMissing.accept("class not found: " + path)));

        // Straight through when a command is already open, and only then hop.
        // The hop is what used to make a bulk edit forty undo entries: a
        // command opened around the list is left behind by the first
        // invokeLater, so every case started one of its own (#153). A nested
        // write command is merged into the open one, which is why the work
        // itself needs no branch.
        if (CommandProcessor.getInstance().getCurrentCommand() != null) inCommand.run();
        else ApplicationManager.getApplication().invokeLater(inCommand);
    }
}
