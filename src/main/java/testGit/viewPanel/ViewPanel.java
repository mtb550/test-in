package testGit.viewPanel;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.content.Content;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.viewPanel.details.DetailsTab;
import testGit.viewPanel.history.HistoryTab;
import testGit.viewPanel.openBugs.OpenBugsTab;

import java.awt.*;
import java.nio.file.Path;

@Getter
public class ViewPanel {
    private final JBPanel<?> detailsTab;
    private final JBPanel<?> historyTab;
    private final JBPanel<?> openBugsTab;

    private TestCaseDto currentTestCaseDto;
    private Path currentPath;

    public ViewPanel() {
        detailsTab = new JBPanel<>(new GridBagLayout());
        historyTab = new JBPanel<>(new BorderLayout());
        openBugsTab = new JBPanel<>(new BorderLayout());
    }

    public static ToolWindow getToolWindow(final Project project) {
        if (project == null) return null;
        return ToolWindowManager.getInstance(project).getToolWindow("TestGitViewPanel");
    }

    public static ToolWindow getToolWindow() {
        return getToolWindow(Config.getProject());
    }

    public static void show(Project project, TestCaseDto testCaseDto, final Path path) {
        ToolWindow tw = getToolWindow(project);
        if (tw == null) return;

        tw.show(() -> {
            ViewPanel viewer = ViewToolWindowFactory.getViewPanel();
            if (viewer != null) {
                selectContent(tw);
                viewer.update(testCaseDto, path);
            }
        });
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
        ViewPanel viewer = ViewToolWindowFactory.getViewPanel();
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

        ViewPanel viewer = ViewToolWindowFactory.getViewPanel();
        if (viewer != null) {
            TestCaseDto currentlyShown = viewer.getCurrentTestCaseDto();

            if (currentlyShown != null && testCaseDtoToMatch != null &&
                    currentlyShown.getId().equals(testCaseDtoToMatch.getId())) {
                reset();
                hide();
            }
        }
    }

    public void update(@Nullable final TestCaseDto testCaseDto, @Nullable final Path path) {
        this.currentTestCaseDto = testCaseDto;
        this.currentPath = path;

        DetailsTab.load(detailsTab, testCaseDto, path);
        HistoryTab.load(historyTab);
        OpenBugsTab.load(openBugsTab);
    }
}