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
import org.testin.util.services.Services;

public class Main implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(final @NotNull Project project, final @NotNull ToolWindow tw) {
        Log.info("ToolWindowFactory.createToolWindowContent()");

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed())
                StartupActivity.execute(project);

            final ProjectPanel projectPanel = Services.getInstance(project, ProjectPanel.class);
            final Content content = ContentFactory.getInstance().createContent(projectPanel.getPanel(), null, false);

            tw.setTitleActions(ProjectPanelActions.create(projectPanel));
            tw.getContentManager().addContent(content);

            Disposer.register(content, projectPanel);
        });
    }
}