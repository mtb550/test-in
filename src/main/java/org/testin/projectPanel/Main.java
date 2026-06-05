package org.testin.projectPanel;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import org.testin.settings.StartupActivity;
import org.testin.util.logger.Log;

public class Main implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        Log.info("ToolWindowFactory.createToolWindowContent()");

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) {
                StartupActivity.execute(project);
            }

            ProjectPanel projectPanel = new ProjectPanel(project);

            toolWindow.setTitleActions(ProjectPanelActions.create(projectPanel));

            Content content = ContentFactory.getInstance().createContent(projectPanel.getPanel(), null, false);
            Disposer.register(content, projectPanel);
            toolWindow.getContentManager().addContent(content);
        });
    }
}