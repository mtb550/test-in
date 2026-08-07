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
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

public class Main implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(final @NotNull Project p, final @NotNull ToolWindow tw) {
        Logger.info("ToolWindowFactory.createToolWindowContent()");

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!p.isDisposed())
                StartupActivity.execute(p);

            final ProjectPanel projectPanel = Services.getInstance(p, ProjectPanel.class);
            final Content content = ContentFactory.getInstance().createContent(projectPanel.getPanel(), null, false);

            tw.setTitleActions(ProjectPanelActions.create(p, projectPanel));
            tw.getContentManager().addContent(content);

            Disposer.register(content, projectPanel);
        });
    }
}