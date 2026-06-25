package org.testin.util.gutter;

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
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;
import org.testin.viewPanel.ViewPanel;
import org.testin.viewPanel.ViewToolWindowFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TestMethodGutter extends RelatedItemLineMarkerProvider implements DumbAware {
    private Project project;

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        this.project = element.getProject();

        if (!(element instanceof PsiJavaToken token) || token.getTokenType() != JavaTokenType.STRING_LITERAL) {
            return;
        }

        PsiLiteralExpression literal = PsiTreeUtil.getParentOfType(token, PsiLiteralExpression.class);
        if (literal == null) return;

        PsiNameValuePair nameValuePair = PsiTreeUtil.getParentOfType(literal, PsiNameValuePair.class);
        if (nameValuePair == null || !"testName".equals(nameValuePair.getName())) return;

        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(nameValuePair, PsiAnnotation.class);
        if (annotation == null || !annotation.getText().contains("@Test")) return;

        String extractedValue = StringUtil.unquoteString(literal.getText()).trim();
        if (extractedValue.isEmpty()) return;

        RelatedItemLineMarkerInfo<PsiElement> marker = new RelatedItemLineMarkerInfo<>(
                element,
                element.getTextRange(),
                AllIcons.Nodes.Related,
                psiElement -> "View Test Case Details",
                (mouseEvent, psiElement) -> openViewPanel(UUID.fromString(extractedValue)),
                GutterIconRenderer.Alignment.RIGHT,
                Collections::emptyList
        );

        result.add(marker);
    }

    private void openViewPanel(final @NotNull UUID uuid) {
        Log.info("Searching for UUID: " + uuid);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);
                indexer.awaitIndexing();

                final TestCaseDto dto = indexer.getTestCaseById(uuid);

                if (dto == null) {
                    Log.error("Test case not found in indexer: " + uuid);
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(project, Notifier.class).warn(project, "Not Found", "No test case found in indexer for ID: " + uuid)
                    );
                    return;
                }

                Log.info("Found in indexer: " + dto.getDescription());

                ApplicationManager.getApplication().invokeLater(() ->
                        ViewToolWindowFactory.showPanel(project, List.of(dto), dto.getParent().getPath2(), ViewPanel::focusDetailsTab)
                );

            } catch (Exception ex) {
                Log.error("Error: " + ex.getMessage());
                ApplicationManager.getApplication().invokeLater(() ->
                        Services.getInstance(project, Notifier.class).error(project, "Error", "Could not find test case: " + ex.getMessage())
                );
            }
        });
    }
}