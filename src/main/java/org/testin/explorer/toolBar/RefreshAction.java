package org.testin.explorer.toolBar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.explorer.ProjectPanel;
import org.testin.services.Services;

import java.util.concurrent.atomic.AtomicBoolean;

public class RefreshAction extends AbstractProjectAction {
    private final @NotNull ProjectPanel pp;
    private final @NotNull AtomicBoolean refreshGuard = new AtomicBoolean(false);

    public RefreshAction(final @NotNull Project p, final @NotNull ProjectPanel pp) {
        super(p, "Refresh", "Re-index and reload tree", AllIcons.Actions.Refresh);
        this.pp = pp;
    }

    public void execute() {
        if (!refreshGuard.compareAndSet(false, true)) {
            Logger.info("Refresh: already in progress, ignoring click");
            return;
        }

        Logger.info("Refresh: re-indexing started");

        final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        indexer.resetForReindex();

        indexer.indexWithProgress();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            indexer.awaitIndexing();

            Logger.info("Refresh: re-indexing complete, rebuilding tree");

            ApplicationManager.getApplication().invokeLater(() -> {
                pp.getTestProjectSelector().loadTestProjectList();

                refreshGuard.set(false);
                Logger.info("Refresh: tree rebuilt");

                // At the end, not the start: the tree is only usable now, and a
                // click that found a refresh already running returned above
                // without saying anything.
                Services.getInstance(p, Notifier.class).softShow(p, "Refreshed");
            });
        });
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - this action has no update() reading Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }
}
