package testGit.projectPanel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import testGit.actions.AddProjectAction;
import testGit.actions.CollapseAllAction;
import testGit.actions.ExpandAllAction;
import testGit.actions.RefreshAction;

import java.util.List;

public class ProjectPanelToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ProjectPanel projectPanel = new ProjectPanel(project);
        Content content = ContentFactory.getInstance().createContent(projectPanel.getPanel(), null, false);
        toolWindow.getContentManager().addContent(content);
        toolWindow.setTitleActions(List.of(contextMenu(projectPanel).getChildren(null)));

        toolWindow.setAutoHide(false);
        //toolWindow.setTitle("TestGit");
        toolWindow.setIcon(AllIcons.Debugger.Db_array);
    }

    private DefaultActionGroup contextMenu(ProjectPanel projectPanel) {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(new ExpandAllAction(projectPanel.getTestCaseTree()));
        group.add(new CollapseAllAction(projectPanel.getTestCaseTree()));
        group.addSeparator();
        group.add(new RefreshAction(projectPanel));
        ///group.add(new SettingsAction()); // no need anymore. to be removed with its class
        group.add(new AddProjectAction(projectPanel));

        return group;
    }
}