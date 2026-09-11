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
     * <b>The content is added here, not in a later event.</b> This is the
     * platform's one call for "fill this tool window", it arrives on the EDT,
     * and the platform builds the window's header and the toolbar of title
     * actions around what it finds when this returns.
     * <p>
     * The whole body used to be inside {@code invokeLater}, so it returned
     * having added nothing at all: the header was built against an empty tool
     * window, and {@code setTitleActions} arrived afterwards. A title button
     * built that way has a data context that cannot answer which project it
     * belongs to, and the platform reads exactly that - {@code event.project!!}
     * in {@code ToolWindowManagerAppLevelHelper} - before <i>any</i> title
     * action runs. It threw, and the button the tester pressed never ran (#66).
     * <p>
     * What is genuinely not the platform's business waits: the startup work
     * reads settings, reads {@code testin.yml} and starts the first index, and
     * none of that is what this call is waiting for.
     */
    @Override
    public void createToolWindowContent(final @NotNull Project p, final @NotNull ToolWindow tw) {
        Logger.info("ToolWindowFactory.createToolWindowContent()");

        final @NotNull TreePanel tp = Services.getInstance(p, TreePanel.class);
        final @NotNull Content content = ContentFactory.getInstance().createContent(tp.getPanel(), null, false);

        // Which component holds the keyboard when this panel is activated.
        // Without it the IDE has nothing to hand focus to - the content is a
        // plain panel and not focusable - so focus stays wherever it was,
        // usually an editor.
        //
        // Not cosmetic. A tree nobody can focus is a tree whose keys are
        // answered by whatever does have the keyboard, and that is how a
        // press over the tree came to be handled by the editor beside it.
        content.setPreferredFocusableComponent(tp.getProjectTree().getMainTree());

        tw.setTitleActions(new TreePanelActions().create(p, tp));
        tw.getContentManager().addContent(content);

        // The panel is a project service, so the project disposes it. It is
        // deliberately not registered as a child of this content as well: that
        // would be a second owner for one lifetime, and closing the content
        // would dispose a service the project container still holds.
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!p.isDisposed()) StartupActivity.execute(p);
        });
    }
}
