package testGit.viewPanel;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;

import java.nio.file.Path;

public class ViewPanel {
    private static TestCaseDto pendingDto = null;
    private static Path pendingPath = null;

    public static ToolWindow getToolWindow(final Project project) {
        if (project == null) return null;
        return ToolWindowManager.getInstance(project).getToolWindow("Details");
    }

    public static ToolWindow getToolWindow() {
        return getToolWindow(Config.getProject());
    }

    public static void show(Project project, TestCaseDto testCaseDto, final Path path) {
        ToolWindow tw = getToolWindow(project);
        if (tw == null) return;

        TestCaseDetailsPanel viewer = ToolWindowFactoryImpl.getDetailsInstance();

        if (viewer != null) {
            if (!tw.isVisible()) tw.show(null);
            selectContent(tw);
            viewer.update(testCaseDto, path);

            pendingDto = null;
            pendingPath = null;
        } else {
            pendingDto = testCaseDto;
            pendingPath = path;
            tw.show(null);
        }
    }

    public static void applyPendingData() {
        TestCaseDetailsPanel viewer = ToolWindowFactoryImpl.getDetailsInstance();
        if (viewer != null && pendingDto != null) {
            viewer.update(pendingDto, pendingPath);

            pendingDto = null;
            pendingPath = null;
        }
    }

    public static void show(final TestCaseDto testCaseDto, final Path path) {
        show(Config.getProject(), testCaseDto, path);
    }

    public static void hide() {
        ToolWindow tw = getToolWindow();
        if (tw != null && tw.isVisible()) {
            tw.hide(null);
        }
    }

    public static void reset() {
        TestCaseDetailsPanel viewer = ToolWindowFactoryImpl.getDetailsInstance();
        if (viewer != null) {
            viewer.update(null, null);
        }
    }

    private static void selectContent(final ToolWindow tw) {
        Content[] contents = tw.getContentManager().getContents();
        for (Content content : contents) {
            if ("Details".equals(content.getDisplayName())) {
                tw.getContentManager().setSelectedContent(content);
                break;
            }
        }
    }

    public static void hideIfShowing(final TestCaseDto testCaseDtoToMatch) {
        ToolWindow tw = getToolWindow();
        if (tw == null || !tw.isVisible()) return;

        TestCaseDetailsPanel viewer = ToolWindowFactoryImpl.getDetailsInstance();
        if (viewer != null) {
            TestCaseDto currentlyShown = viewer.getCurrentTestCaseDto();

            if (currentlyShown != null && testCaseDtoToMatch != null &&
                    currentlyShown.getId().equals(testCaseDtoToMatch.getId())) {
                reset();
                hide();
            }
        }
    }
}