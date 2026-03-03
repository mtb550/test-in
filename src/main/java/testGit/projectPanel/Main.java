package testGit.projectPanel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class Main implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        System.out.println("ToolWindowFactory.createToolWindowContent()");

        toolWindow.setIcon(AllIcons.Debugger.Db_array);

        ProjectPanel projectPanel = new ProjectPanel(project);

        toolWindow.setTitleActions(TitleActions.create(projectPanel));

        Content content = ContentFactory.getInstance().createContent(projectPanel.getPanel(), null, false);
        Disposer.register(content, projectPanel);
        toolWindow.getContentManager().addContent(content);

        projectPanel.init(project);
    }
}