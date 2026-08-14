package org.testin.projectPanel.toolBar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.projectPanel.ProjectPanel;
import org.testin.services.Services;

import java.util.concurrent.atomic.AtomicBoolean;

public class RefreshAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull ProjectPanel pp;
    private final @NotNull AtomicBoolean refreshGuard = new AtomicBoolean(false);

    public RefreshAction(final @NotNull Project p, final @NotNull ProjectPanel pp) {
        super("Refresh", "Re-index and reload tree", AllIcons.Actions.Refresh);
        this.p = p;
        this.pp = pp;
    }

    public void execute() {
        if (!refreshGuard.compareAndSet(false, true)) {
            Logger.info("Refresh: already in progress, ignoring click");
            return;
        }

        Logger.info("Refresh: re-indexing started");
        Services.getInstance(p, Notifier.class).softShow(p, "Refreshing..");

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
