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
import org.testin.pojo.Config;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.Mapper;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;
import org.testin.viewPanel.ViewToolWindowFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class TestMethodGutter extends RelatedItemLineMarkerProvider implements DumbAware {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
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
                (mouseEvent, psiElement) -> openViewPanel(element.getProject(), extractedValue),
                GutterIconRenderer.Alignment.RIGHT,
                Collections::emptyList
        );

        result.add(marker);
    }

    private void openViewPanel(Project project, String targetId) {
        Path rootPath = Config.getTestinPath();

        Log.info("[GUTTER TRACE] Root path: " + rootPath);
        Log.info("[GUTTER TRACE] Searching for UUID: " + targetId);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                File foundFile;
                try (Stream<Path> stream = Files.walk(rootPath)) {
                    foundFile = stream
                            .filter(p -> p.getFileName().toString().equals(targetId + ".json"))
                            .map(Path::toFile)
                            .findFirst()
                            .orElse(null);
                }

                if (foundFile == null) {
                    Log.error("[GUTTER TRACE] File not found in external path: " + targetId + ".json");
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().warn(project, "Not Found", "No JSON file found in external path for ID: " + targetId)
                    );
                    return;
                }

                Log.info("[GUTTER TRACE] Found file: " + foundFile.getAbsolutePath());

                TestCaseDto dto = Services.getInstance(project, Mapper.class).readValue(foundFile, TestCaseDto.class);
                Path parentPath = foundFile.getParentFile().toPath();

                ApplicationManager.getApplication().invokeLater(() -> ViewToolWindowFactory.showPanel(project, List.of(dto), parentPath));

            } catch (Exception ex) {
                Log.error("[GUTTER TRACE] IO Error: " + ex.getMessage());
                ApplicationManager.getApplication().invokeLater(() ->
                        Notifier.getInstance().error(project, "Error", "Could not read JSON file: " + ex.getMessage())
                );
            }
        });
    }
}