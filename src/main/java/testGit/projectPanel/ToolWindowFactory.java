package testGit.projectPanel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import testGit.actions.*;

import java.util.List;

public class ToolWindowFactory implements com.intellij.openapi.wm.ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ProjectPanel projectPanel = new ProjectPanel(project);
        Content content = ContentFactory.getInstance().createContent(projectPanel.getPanel(), null, false);
        toolWindow.getContentManager().addContent(content);
        toolWindow.setTitleActions(List.of(contextMenu(projectPanel).getChildren(ActionManager.getInstance())));

        toolWindow.setAutoHide(false);
        //toolWindow.setTitle("TestGit");
        toolWindow.setIcon(AllIcons.Debugger.Db_array);
    }

    private DefaultActionGroup contextMenu(ProjectPanel projectPanel) {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(new OpenSettings());
        group.add(new ExpandAll(ProjectPanel.testCaseTree));
        group.add(new CollapseAll(ProjectPanel.testCaseTree));
        group.add(new Refresh(projectPanel));
        group.add(new CreateTestProject(projectPanel));

        return group;
    }
}