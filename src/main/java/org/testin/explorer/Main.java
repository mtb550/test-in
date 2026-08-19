package org.testin.explorer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.services.Services;
import org.testin.setting.StartupActivity;

public class Main implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(final @NotNull Project p, final @NotNull ToolWindow tw) {
        Logger.info("ToolWindowFactory.createToolWindowContent()");

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!p.isDisposed())
                StartupActivity.execute(p);

            final ExplorerPanel pp = Services.getInstance(p, ExplorerPanel.class);
            final Content content = ContentFactory.getInstance().createContent(pp.getPanel(), null, false);

            tw.setTitleActions(new ExplorerPanelActions().create(p, pp));
            tw.getContentManager().addContent(content);

            Disposer.register(content, pp);
        });
    }
}