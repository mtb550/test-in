package testGit.viewPanel;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;

public class ViewToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Getter
    private static ViewPanel viewPanel;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        /// to be added for all other components. tree, editor. as we may run one of them first
        Config.setProject(project);

        viewPanel = new ViewPanel();

        ContentFactory contentFactory = ContentFactory.getInstance();

        Content detailsTab = contentFactory.createContent(viewPanel.getDetailsTab(), "Details", false);
        Content historyTab = contentFactory.createContent(viewPanel.getHistoryTab(), "History", false);
        Content bugsTab = contentFactory.createContent(viewPanel.getOpenBugsTab(), "Open Bugs", false);

        toolWindow.getContentManager().addContent(detailsTab);
        toolWindow.getContentManager().addContent(historyTab);
        toolWindow.getContentManager().addContent(bugsTab);
    }
}