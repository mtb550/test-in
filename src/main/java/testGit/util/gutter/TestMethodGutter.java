package testGit.util.gutter;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.Notifications.Notifier;
import testGit.viewPanel.ViewToolWindowFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TestMethodGutter extends RelatedItemLineMarkerProvider implements DumbAware {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!(element instanceof PsiJavaToken token) ||
                token.getTokenType() != JavaTokenType.STRING_LITERAL) {
            return;
        }

        PsiLiteralExpression literal = PsiTreeUtil.getParentOfType(token, PsiLiteralExpression.class);
        if (literal == null) return;

        PsiNameValuePair nameValuePair = PsiTreeUtil.getParentOfType(literal, PsiNameValuePair.class);
        if (nameValuePair == null || !"description".equals(nameValuePair.getName())) return;

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
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final VirtualFile[] fileRef = new VirtualFile[1];

            ApplicationManager.getApplication().runReadAction(() -> {
                Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(
                        targetId + ".json",
                        GlobalSearchScope.projectScope(project)
                );
                if (!files.isEmpty()) fileRef[0] = files.iterator().next();
            });

            if (fileRef[0] == null) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Notifier.warn("Not Mapped", "No JSON file mapped for description: " + targetId)
                );
                return;
            }

            try {
                File file = new File(fileRef[0].getPath());
                TestCaseDto dto = Config.getMapper().readValue(file, TestCaseDto.class);
                Path path = file.getParentFile().toPath();

                ApplicationManager.getApplication().invokeLater(() -> ViewToolWindowFactory.showPanel(project, List.of(dto), path));

            } catch (Exception ex) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Notifier.error("Error", "Could not read JSON file.")
                );
            }
        });
    }
}