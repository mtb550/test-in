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
import org.jetbrains.annotations.Nullable;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TestMethodGutter extends RelatedItemLineMarkerProvider implements DumbAware {

    /**
     * Null when the annotation's testName is not a UUID at all.
     */
    private static @Nullable UUID parseUuid(final @NotNull String value) {
        try {
            return UUID.fromString(value);
        } catch (final IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        final @NotNull Project p = element.getProject();

        if (!(element instanceof PsiJavaToken token) || token.getTokenType() != JavaTokenType.STRING_LITERAL) {
            return;
        }

        PsiLiteralExpression literal = PsiTreeUtil.getParentOfType(token, PsiLiteralExpression.class);
        if (literal == null) return;

        PsiNameValuePair nameValuePair = PsiTreeUtil.getParentOfType(literal, PsiNameValuePair.class);
        if (nameValuePair == null || !"testName".equals(nameValuePair.getName())) return;

        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(nameValuePair, PsiAnnotation.class);
        if (annotation == null || !annotation.hasQualifiedName("org.testng.annotations.Test")) return;

        String extractedValue = StringUtil.unquoteString(literal.getText()).trim();
        if (extractedValue.isEmpty()) return;

        // Only mark testin-managed methods: a handwritten testName like "smoke"
        // is not a UUID and clicking its marker would throw.
        final UUID testCaseId = parseUuid(extractedValue);
        if (testCaseId == null) return;

        RelatedItemLineMarkerInfo<PsiElement> marker = new RelatedItemLineMarkerInfo<>(
                element,
                element.getTextRange(),
                AllIcons.Nodes.Related,
                psiElement -> "View Test Case Details",
                (mouseEvent, psiElement) -> openViewPanel(p, testCaseId),
                GutterIconRenderer.Alignment.RIGHT,
                Collections::emptyList
        );

        result.add(marker);
    }

    private void openViewPanel(final @NotNull Project p, final @NotNull UUID uuid) {
        Logger.info("Searching for UUID: " + uuid);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                indexer.awaitIndexing();

                final TestCaseDto dto = indexer.getTestCaseById(uuid);

                if (dto == null) {
                    Logger.error("Unable to find test case with UUID: " + uuid);
                    return;
                }

                Logger.info("Found in indexer: " + dto.getDescription());

                ApplicationManager.getApplication().invokeLater(() ->
                        ViewToolWindowFactory.showPanel(p, List.of(dto), dto.getParent().getPath2(), ViewPanel::focusDetailsTab)
                );

            } catch (final Exception ex) {
                Logger.error("Error: " + ex.getMessage());
                ApplicationManager.getApplication().invokeLater(() ->
                        Services.getInstance(p, Notifier.class).error(p, "Error", "Could not find test case: " + ex.getMessage())
                );
            }
        });
    }
}