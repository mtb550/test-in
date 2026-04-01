package testGit.projectPanel;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import testGit.settings.StartupActivity;
import testGit.settings.service.ProjectPanelService;

public class Main implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        System.out.println("ToolWindowFactory.createToolWindowContent()");

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) {
                StartupActivity.execute(project);
            }

            ProjectPanel projectPanel = new ProjectPanel(project);

            toolWindow.setTitleActions(ProjectPanelActions.create(projectPanel));

            ProjectPanelService.getInstance(project).setPanel(projectPanel);

            Content content = ContentFactory.getInstance().createContent(projectPanel.getPanel(), null, false);
            Disposer.register(content, projectPanel);
            toolWindow.getContentManager().addContent(content);
        });
    }
}