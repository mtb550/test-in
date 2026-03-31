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
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.Notifier;
import testGit.viewPanel.ViewPanel;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TestMethodGutter extends RelatedItemLineMarkerProvider implements DumbAware {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!(element instanceof PsiLiteralExpression)) return;

        String content = element.getText();
        if (content == null || content.length() < 36) return;

        String uuidStr = StringUtil.unquoteString(content);
        if (uuidStr.length() != 36) return;

        PsiNameValuePair nameValuePair = PsiTreeUtil.getParentOfType(element, PsiNameValuePair.class);
        if (nameValuePair == null || !"description".equals(nameValuePair.getName())) return;

        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(nameValuePair, PsiAnnotation.class);
        if (annotation == null || !annotation.getText().contains("@Test")) return;

        RelatedItemLineMarkerInfo<PsiElement> marker = new RelatedItemLineMarkerInfo<>(
                element,
                element.getTextRange(),
                AllIcons.Nodes.Related,
                psiElement -> "View Test Case Details",
                (mouseEvent, psiElement) -> openViewPanel(element.getProject(), uuidStr),
                GutterIconRenderer.Alignment.RIGHT,
                Collections::emptyList
        );

        result.add(marker);
    }

    private void openViewPanel(Project project, String uuidStr) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final VirtualFile[] fileRef = new VirtualFile[1];

            ApplicationManager.getApplication().runReadAction(() -> {
                Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(
                        uuidStr + ".json",
                        GlobalSearchScope.projectScope(project)
                );
                if (!files.isEmpty()) fileRef[0] = files.iterator().next();
            });

            if (fileRef[0] == null) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Notifier.warn("Not Found", "No JSON file found for ID: " + uuidStr)
                );
                return;
            }

            try {
                File file = new File(fileRef[0].getPath());
                TestCaseDto dto = Config.getMapper().readValue(file, TestCaseDto.class);
                Path path = file.getParentFile().toPath();

                ApplicationManager.getApplication().invokeLater(() ->
                        ViewPanel.show(project, List.of(dto), path)
                );
            } catch (Exception ex) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Notifier.error("Error", "Could not read JSON file.")
                );
            }
        });
    }
}