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
import testGit.viewPanel.details.DetailsTab;

public class ToolWindowFactoryImpl implements ToolWindowFactory, DumbAware {
    @Getter
    private static DetailsTab detailsInstance;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        Config.setProject(project); /// to be added for all other components. tree, editor. as we may run one of them first

        detailsInstance = new DetailsTab();

        ContentFactory contentFactory = ContentFactory.getInstance();

        Content detailsTab = contentFactory.createContent(detailsInstance.getDetailsTab(), "Details", false);
        Content historyTab = contentFactory.createContent(detailsInstance.getHistoryTab(), "History", false);
        Content bugsTab = contentFactory.createContent(detailsInstance.getBugTab(), "Open Bugs", false);

        toolWindow.getContentManager().addContent(detailsTab);
        toolWindow.getContentManager().addContent(historyTab);
        toolWindow.getContentManager().addContent(bugsTab);
    }
}