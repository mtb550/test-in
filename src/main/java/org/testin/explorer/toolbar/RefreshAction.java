package org.testin.explorer.toolbar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.config.TestinConfigService;
import org.testin.explorer.ExplorerPanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.util.EditorUtil;
import org.testin.services.Services;

import java.util.concurrent.atomic.AtomicBoolean;

public class RefreshAction extends AbstractProjectAction {

    /**
     * What the toolbar button reports when it is the tester pressing Refresh.
     */
    private static final @NotNull String REFRESHED = "Refreshed";

    private final @NotNull ExplorerPanel pp;

    /**
     * Stops a second re-index starting through one already running.
     * <p>
     * It only works because there is one of these per project, held on
     * {@link ExplorerPanel}. Constructing one to call {@code execute} gives it a
     * guard nobody else can see, which is what this used to be.
     */
    private final @NotNull AtomicBoolean refreshGuard = new AtomicBoolean(false);

    public RefreshAction(final @NotNull Project p, final @NotNull ExplorerPanel pp) {
        super(p, "Refresh", "Re-index and reload tree", AllIcons.Actions.Refresh);
        this.pp = pp;
    }

    public void execute() {
        execute(REFRESHED);
    }

    /**
     * Re-indexes and rebuilds the tree, reporting the outcome in the caller's
     * words.
     * <p>
     * A branch switch is this action with a different sentence at the end: the
     * work is identical - re-index, rebuild, close what is gone - and the only
     * thing the tester needs told apart is what caused it. One notification
     * either way, because two would be one too many for one press.
     */
    public void execute(final @NotNull String outcome) {
        if (!refreshGuard.compareAndSet(false, true)) {
            Logger.info("Refresh: already in progress, ignoring click");
            return;
        }

        Logger.info("Refresh: re-indexing started");

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // The repository's testin.yml is on disk too, and Refresh is the
            // tester saying "read the disk again". It was read once when the
            // service was created and never after, so a file that was deleted,
            // hand-edited, or brought in by a branch switch left the plugin
            // acting on what it said at startup (#6).
            //
            // Before the index, exactly as at startup: the file names the test
            // project, and indexing is scoped to it.
            Services.getInstance(p, TestinConfigService.class).reload();

            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
            indexer.resetForReindex();
            indexer.indexWithProgress();
            indexer.awaitIndexing();

            Logger.info("Refresh: re-indexing complete, rebuilding tree");

            ApplicationManager.getApplication().invokeLater(() -> {
                if (p.isDisposed()) {
                    refreshGuard.set(false);
                    return;
                }

                // Before the tree is rebuilt: an editor is holding the node
                // it was opened on and the cases it read from it, and after a
                // re-index either can be data that is gone.
                Services.getInstance(p, EditorUtil.class).refreshOpen(p);

                pp.refresh();

                refreshGuard.set(false);
                Logger.info("Refresh: tree rebuilt");

                // At the end, not the start: the tree is only usable now, and a
                // click that found a refresh already running returned above
                // without saying anything.
                Services.getInstance(p, Notifier.class).softShow(p, outcome);
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
