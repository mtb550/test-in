package org.testin.java.gutter;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.search.GoTo;
import org.testin.search.Hit;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class TestMethodGutter extends RelatedItemLineMarkerProvider implements DumbAware {

    /**
     * Empty when the annotation's testName is not a UUID at all.
     */
    private static @NotNull Optional<UUID> parseUuid(final @NotNull String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (final IllegalArgumentException notAnId) {
            return Optional.empty();
        }
    }

    // UC-CODEGEN-007, Rule-CODEGEN-028, Rule-CODEGEN-029
    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        final @NotNull Project p = element.getProject();

        if (!(element instanceof PsiJavaToken token) || token.getTokenType() != JavaTokenType.STRING_LITERAL) {
            return;
        }

        // Only mark testin-managed methods: a string that is not the testName
        // of a TestNG @Test is ordinary code, and a handwritten testName like
        // "smoke" is not a UUID - clicking its marker would throw.
        Optional.ofNullable(PsiTreeUtil.getParentOfType(token, PsiLiteralExpression.class))
                .filter(TestMethodGutter::namesATestCase)
                .map(literal -> StringUtil.unquoteString(literal.getText()).trim())
                .flatMap(TestMethodGutter::parseUuid)
                .ifPresent(testCaseId -> result.add(new RelatedItemLineMarkerInfo<>(
                element,
                element.getTextRange(),
                AllIcons.Nodes.Related,
                psiElement -> "Go to Test Case",
                (mouseEvent, psiElement) -> openViewPanel(p, testCaseId, methodName(psiElement)),
                GutterIconRenderer.Alignment.RIGHT,
                Collections::emptyList
        )));
    }

    /**
     * UC-CODEGEN-007, Rule-CODEGEN-029.
     * <p>
     * Whether this string literal is the testName of a TestNG @Test. Each step
     * up the tree can run out of parents, and running out means the same as
     * finding the wrong thing: not ours.
     */
    private static boolean namesATestCase(final @NotNull PsiLiteralExpression literal) {
        return Optional.ofNullable(PsiTreeUtil.getParentOfType(literal, PsiNameValuePair.class))
                .filter(pair -> "testName".equals(pair.getName()))
                .map(pair -> PsiTreeUtil.getParentOfType(pair, PsiAnnotation.class))
                .filter(annotation -> annotation.hasQualifiedName("org.testng.annotations.Test"))
                .isPresent();
    }

    /**
     * The generated method the mark sits on, for the sentence a refusal needs.
     * Read on the click rather than kept, because a marker outlives several edits
     * of the method around it.
     */
    private static @NotNull String methodName(final @NotNull PsiElement element) {
        return Optional.ofNullable(PsiTreeUtil.getParentOfType(element, PsiMethod.class)).map(PsiMethod::getName).orElse("This method");
    }

    // UC-CODEGEN-007, Rule-CODEGEN-030, Rule-CODEGEN-069
    private void openViewPanel(final @NotNull Project p, final @NotNull UUID uuid, final @NotNull String methodName) {
        Logger.info("Searching for UUID: " + uuid);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                indexer.awaitIndexing();

                // Generated code outlives the case it was generated from: the
                // annotation still names an id nobody can open.
                indexer.findTestCase(uuid).ifPresentOrElse(
                        dto -> {
                            Logger.info("Found in indexer: " + dto.getDescription());

                            // Where the tester works, not just what the case says.
                            // This opened the details panel and stopped there, so
                            // reading a generated method told you about the test
                            // case and left you nowhere near it. GoTo is the one
                            // that takes somebody somewhere: the tree expands to
                            // the test set, the editor opens on it, and the row
                            // is selected - the same three things the global
                            // search does for a case it found.
                            ApplicationManager.getApplication().invokeLater(() -> GoTo.the(p, Hit.of(dto)));
                        },
                        () -> refuseMissingCase(p, uuid, methodName));

            } catch (final Exception ex) {
                // Named for what failed rather than titled "Error", which said
                // nothing the message did not and was the same word two other
                // files write. Found the day the gate first read this module
                // (#170).
                Logger.error("Could not open the test case behind this mark: " + ex.getMessage());
                ApplicationManager.getApplication().invokeLater(() ->
                        Services.getInstance(p, Notifier.class).error(p, "Test Case Not Opened", "Could not find test case: " + ex.getMessage())
                );
            }
        });
    }

    /**
     * UC-CODEGEN-007, Rule-CODEGEN-069.
     * <p>
     * The click found nothing, which is an ordinary answer rather than a
     * failure: generated code outlives the test case it was written from, so a
     * method whose case has been removed still carries the mark and still names
     * an id. It went to the log alone, and the tester clicking got no answer at
     * all (#245).
     * <p>
     * A soft refusal rather than a notification that stays, because it is
     * feedback on the click just made and the remedy - open the test set, or
     * delete the method - is theirs either way.
     */
    private static void refuseMissingCase(final @NotNull Project p, final @NotNull UUID uuid, final @NotNull String methodName) {
        Logger.info("No test case behind " + methodName + ": " + uuid);

        ApplicationManager.getApplication().invokeLater(() ->
                Services.getInstance(p, Notifier.class).softRefuse(p, Refused.NO_TEST_CASE_BEHIND_IT, methodName));
    }
}
