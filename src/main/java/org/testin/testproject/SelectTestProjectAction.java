package org.testin.testproject;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.explorer.ExplorerPanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.ProjectStatus;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;

import java.util.Map;

/**
 * Changes which test project this repository is bound to (#8).
 * <p>
 * The way back. A repository names one project and the panel shows only that
 * one, which is the point - but a tester who bound the wrong one, or whose
 * project was renamed, would otherwise have to edit {@code testin.yml} by hand
 * to say so. One toolbar button, not a dropdown: choosing is a thing done twice
 * in a repository's life, not on every glance at the tree.
 */
public final class SelectTestProjectAction extends AbstractProjectAction {

    private final @NotNull ExplorerPanel pp;

    public SelectTestProjectAction(final @NotNull Project p, final @NotNull ExplorerPanel pp) {
        super(p, "Select Test Project", "Choose the test project this repository exercises", AllIcons.Actions.ModuleDirectory);
        this.pp = pp;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Map<String, ProjectStatus> underRoot = Services.getInstance(p, ProjectIndexer.class).testProjects();

        // An empty picker would say nothing at all. The button beside this one
        // is the answer, so the message points at it rather than opening a
        // dialog with no rows in it.
        if (underRoot.isEmpty()) {
            Services.getInstance(p, Notifier.class).softShow(p, "No Test Projects",
                    "Create one under the Testin root first");
            return;
        }

        new BindTestProjectDialog(p, underRoot, pp::reindex).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        // There is nothing to choose from without a root. Both branches, so the
        // button comes back once one is configured.
        e.getPresentation().setEnabled(!Services.getInstance(p, TestinRoot.class).getPath().toString().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - update() reads a setting, never Swing state (#52).
        return ActionUpdateThread.BGT;
    }
}
