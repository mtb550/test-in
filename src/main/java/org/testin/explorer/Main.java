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

    // UC-TREE-PANEL-001
    @Override
    public void createToolWindowContent(final @NotNull Project p, final @NotNull ToolWindow tw) {
        Logger.info("ToolWindowFactory.createToolWindowContent()");

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!p.isDisposed())
                StartupActivity.execute(p);

            final @NotNull TreePanel tp = Services.getInstance(p, TreePanel.class);
            final @NotNull Content content = ContentFactory.getInstance().createContent(tp.getPanel(), null, false);

            // UC-TREE-PANEL-001, Rule-TREE-PANEL-097.
            //
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

            Disposer.register(content, tp);
        });
    }
}