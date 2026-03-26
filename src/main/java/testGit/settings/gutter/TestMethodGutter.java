package testGit.settings.gutter;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.viewPanel.ViewPanel;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

public class TestMethodGutter extends RelatedItemLineMarkerProvider {
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
                (mouseEvent, psiElement) -> openViewPanel(psiElement, method, uuidStr),
                GutterIconRenderer.Alignment.LEFT,
                Collections::emptyList
        );

        result.add(marker);
    }

    private void openViewPanel(PsiElement element, PsiMethod method, String uuid) {
        Project project = element.getProject();
        PsiClass psiClass = method.getContainingClass();
        if (psiClass == null || psiClass.getName() == null) return;

        String basePath = project.getBasePath();
        if (basePath == null) return;

        File testGitDir = new File(basePath, "testGit");
        if (!testGitDir.exists() || !testGitDir.isDirectory()) return;

        File[] projectDirs = testGitDir.listFiles(File::isDirectory);
        if (projectDirs == null) return;

        File targetJsonFile = null;

        for (File dir : projectDirs) {
            String dirName = dir.getName();
            if (dirName.endsWith("_AC") || dirName.endsWith("_IN") || dirName.endsWith("_AR") || dirName.endsWith("_RE")) {

                File jsonFile = new File(dir, "testCases/" + psiClass.getName() + "/" + uuid + ".json");
                if (jsonFile.exists()) {
                    targetJsonFile = jsonFile;
                    break;
                }
            }
        }

        if (targetJsonFile != null) {
            try {
                TestCaseDto dto = Config.getMapper().readValue(targetJsonFile, TestCaseDto.class);
                ViewPanel.show(project, dto);
            } catch (Exception ex) {
                System.err.println("Failed to read JSON: " + targetJsonFile.getAbsolutePath());
                ex.printStackTrace(System.out);
            }
        } else {
            System.err.println("Test Case file not found for UUID: " + uuid);
        }
    }
}