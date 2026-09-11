package org.testin.explorer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.services.Services;
import org.testin.setting.StartupActivity;

public class Main implements ToolWindowFactory, DumbAware {

    /**
     * UC-TREE-PANEL-001, Rule-TREE-PANEL-097.
     * <p>
     * Fills the tree tool window: the panel, the key it hands focus to, and the
     * buttons in its title bar.
     * <p>
     * All of it here rather than in a later event, because this is the
     * platform's one call for it and it already arrives on the EDT. Only the
     * startup work waits - it reads settings, reads {@code testin.yml} and
     * starts the first index, none of which the platform is waiting for.
     */
    @Override
    public void createToolWindowContent(final @NotNull Project p, final @NotNull ToolWindow tw) {
        Logger.info("ToolWindowFactory.createToolWindowContent()");

        final @NotNull TreePanel tp = Services.getInstance(p, TreePanel.class);
        // The panel is a project service, so the project disposes it. It is
        // deliberately not registered as a child of this content as well: that
        // would be a second owner for one lifetime, and closing the content
        // would dispose a service the project container still holds.
        final @NotNull Content content = ContentFactory.getInstance().createContent(tp.getPanel(), null, false);

        // Which component the IDE hands the keyboard to, and aims the title
        // bar at. It is the tree whenever there is one - a tree nobody can
        // focus is a tree whose keys are answered by whatever does have the
        // keyboard - and the panel while the welcome screen is up, because a
        // hidden component is one the platform will not run a title action
        // against. The panel answers it, being what knows which of the two it
        // is drawing.
        tp.showIn(content);

        tw.setTitleActions(new TreePanelActions().create(p, tp));
        tw.getContentManager().addContent(content);

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!p.isDisposed()) StartupActivity.execute(p);
        });
    }
}
