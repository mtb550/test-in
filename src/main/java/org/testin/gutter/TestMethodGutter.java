package org.testin.gutter;

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
import org.testin.services.Services;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
                psiElement -> "View Test Case Details",
                (mouseEvent, psiElement) -> openViewPanel(p, testCaseId),
                GutterIconRenderer.Alignment.RIGHT,
                Collections::emptyList
        )));
    }

    /**
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

    private void openViewPanel(final @NotNull Project p, final @NotNull UUID uuid) {
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
                            ApplicationManager.getApplication().invokeLater(() ->
                                    ViewToolWindowFactory.showPanel(p, List.of(dto), dto.getParent().getPath2(), ViewPanel::focusDetailsTab));
                        },
                        () -> Logger.error("Unable to find test case with UUID: " + uuid));

            } catch (final Exception ex) {
                Logger.error("Error: " + ex.getMessage());
                ApplicationManager.getApplication().invokeLater(() ->
                        Services.getInstance(p, Notifier.class).error(p, "Error", "Could not find test case: " + ex.getMessage())
                );
            }
        });
    }
}