package testGit.util.gutter;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.viewPanel.ViewPanel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

public class TestMethodGutter extends RelatedItemLineMarkerProvider implements DumbAware {
    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!(element instanceof PsiIdentifier)) return;

        PsiElement parent = element.getParent();
        if (!(parent instanceof PsiJavaCodeReferenceElement)) return;

        PsiElement grandParent = parent.getParent();
        if (!(grandParent instanceof PsiAnnotation annotation)) return;

        String qualifiedName = annotation.getQualifiedName();
        if (qualifiedName == null || !qualifiedName.endsWith(".Test")) return;

        PsiAnnotationMemberValue descValue = annotation.findDeclaredAttributeValue("description");
        if (descValue == null) return;

        String uuidStr = StringUtil.unquoteString(descValue.getText());
        if (uuidStr.length() != 36 || uuidStr.split("-").length != 5) return;

        PsiElement modifierList = annotation.getParent();
        if (!(modifierList instanceof PsiModifierList) || !(modifierList.getParent() instanceof PsiMethod method))
            return;

        RelatedItemLineMarkerInfo<PsiElement> marker = new RelatedItemLineMarkerInfo<>(
                element,
                element.getTextRange(),
                AllIcons.Nodes.Related,
                psiElement -> "View Test Case Details",
                (mouseEvent, psiElement) -> openViewPanel(psiElement, uuidStr),
                GutterIconRenderer.Alignment.RIGHT,
                Collections::emptyList
        );

        result.add(marker);
    }

    private void openViewPanel(PsiElement element, String uuid) {
        Project project = element.getProject();
        String basePath = project.getBasePath();
        if (basePath == null) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            File testGitDir = new File(basePath, "testGit");
            if (!testGitDir.exists() || !testGitDir.isDirectory()) {
                System.err.println("testGit directory not found at: " + testGitDir.getAbsolutePath());
                return;
            }

            File targetJsonFile = null;
            String targetFileName = uuid + ".json";

            try (Stream<Path> stream = Files.walk(testGitDir.toPath())) {
                Path foundPath = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().equals(targetFileName))
                        .findFirst()
                        .orElse(null);

                if (foundPath != null) {
                    targetJsonFile = foundPath.toFile();
                }
            } catch (IOException e) {
                System.err.println("Error searching for JSON file: " + e.getMessage());
            }

            if (targetJsonFile != null) {
                try {
                    TestCaseDto dto = Config.getMapper().readValue(targetJsonFile, TestCaseDto.class);
                    Path testSetPath = targetJsonFile.getParentFile().toPath();
                    ApplicationManager.getApplication().invokeLater(() -> ViewPanel.show(project, dto, testSetPath));

                } catch (Exception ex) {
                    System.err.println("Failed to read JSON: " + targetJsonFile.getAbsolutePath());
                    ex.printStackTrace(System.out);
                }
            } else {
                System.err.println("Test Case file not found for UUID: " + uuid);
            }
        });
    }
}