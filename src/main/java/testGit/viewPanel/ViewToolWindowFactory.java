package testGit.viewPanel;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class ViewToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Getter
    private static ViewPanel viewPanel;

    public static ToolWindow getToolWindow(final Project project) {
        if (project == null) return null;
        return ToolWindowManager.getInstance(project).getToolWindow("TestGitViewPanel");
    }

    public static ToolWindow getToolWindow() {
        return getToolWindow(Config.getProject());
    }

    public static void showPanel(final Project project, final List<TestCaseDto> testCases, final Path path, final Consumer<ViewPanel> onReadyAction) {
        ToolWindow tw = getToolWindow(project);

        if (tw != null) {
            tw.show(() -> {
                ViewPanel viewer = getViewPanel();
                if (viewer != null) {
                    viewer.show(testCases, path);

                    if (onReadyAction != null) {
                        onReadyAction.accept(viewer);
                    }
                }
            });
        }
    }

    public static void showPanel(final Project project, final List<TestCaseDto> testCases, final Path path) {
        showPanel(project, testCases, path, null);
    }

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        Config.setProject(project);

        viewPanel = new ViewPanel();

        ContentFactory contentFactory = ContentFactory.getInstance();

        Content detailsTab = contentFactory.createContent(viewPanel.getDetailsTab(), "Details", false);
        Content historyTab = contentFactory.createContent(viewPanel.getHistoryTab(), "History", false);
        Content bugsTab = contentFactory.createContent(viewPanel.getOpenBugsTab(), "Open Bugs", false);

        toolWindow.getContentManager().addContent(detailsTab);
        toolWindow.getContentManager().addContent(historyTab);
        toolWindow.getContentManager().addContent(bugsTab);

        toolWindow.setTitleActions(ViewPanelActions.create(viewPanel.getPage(), toolWindow.getComponent()));
    }
}