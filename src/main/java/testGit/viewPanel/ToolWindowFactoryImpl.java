package testGit.viewPanel;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class ToolWindowFactoryImpl implements ToolWindowFactory, DumbAware {
    @Getter
    private static TestCaseDetailsPanel detailsInstance;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        detailsInstance = new TestCaseDetailsPanel();

        ContentFactory contentFactory = ContentFactory.getInstance();

        Content detailsTab = contentFactory.createContent(detailsInstance.getDetailsTab(), "Details", false);
        Content historyTab = contentFactory.createContent(detailsInstance.getHistoryTab(), "History", false);
        Content bugsTab = contentFactory.createContent(detailsInstance.getBugTab(), "Open Bugs", false);

        toolWindow.getContentManager().addContent(detailsTab);
        toolWindow.getContentManager().addContent(historyTab);
        toolWindow.getContentManager().addContent(bugsTab);

    }
}