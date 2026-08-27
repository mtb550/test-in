package org.testin.git;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import javax.swing.tree.TreePath;
import java.util.Optional;
import java.nio.file.Path;
import java.util.List;

public class SyncActionAction extends AbstractProjectTreeAction {
    private final @NotNull GitRepositoryService git;

    /**
     * For the push at the end. The same service the review pushes through, so
     * there is one way commits leave this machine rather than two.
     */
    private final @NotNull GitCommitService commits;

    public SyncActionAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, tree, "Sync With Remote", "Pull the latest test cases, then push anything committed here", AllIcons.Actions.SyncPanels);
        this.git = new GitRepositoryService(p);
        this.commits = new GitCommitService(p);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        getActiveProjectPath().ifPresentOrElse(this::syncRepository, () ->
                Services.getInstance(p, Notifier.class).error(p, "Sync Error",
                        "Could not determine the active project. Please select a project in the tree."));
    }

    /**
     * Everything the action does once it knows which repository it is syncing.
     */
    private void syncRepository(final @NotNull Path repoPath) {

        // Soft, and not an error: nothing failed. The tester pressed Sync on a
        // test project that was never put under Git, and the sentence says which
        // project and where the repository comes from - the review is what
        // offers to create one.
        if (git.isNotRepository(repoPath)) {
            Services.getInstance(p, Notifier.class).softShow(p, "Nothing to Sync",
                    "'" + repoPath.getFileName() + "' is not under Git yet. Open Pending Commits to create the repository.");
            return;
        }

        GitBackgroundTask.run(p, "Syncing with remote", true,
                indicator -> {
                    indicator.setText("Checking remote configuration...");
                    final @NotNull String remoteName = git.getRemoteName(repoPath);
                    final @NotNull String remoteUrl = remoteName.isEmpty() ? "" : git.getRemoteUrl(repoPath, remoteName);

                    if (remoteUrl.isEmpty()) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).warn(p, "Sync Aborted", "No remote URL is configured for this project. Push a commit first to configure the remote.")
                        );
                        return;
                    }

                    final @NotNull String branch = git.syncBranch(repoPath);
                    if (branch.isBlank()) {
                        throw new IllegalStateException("Could not determine which branch to sync.");
                    }

                    // A pull into a rebase that is still halfway through is
                    // refused by Git with a fatal about a leftover directory,
                    // which is a sentence about Git's internals shown to someone
                    // who pressed Sync. The state is already knowable, so it is
                    // asked before the pull rather than read out of its failure.
                    if (git.hasConflicts(repoPath)) {
                        final @NotNull List<String> unfinished = git.conflictingPaths(repoPath);
                        ApplicationManager.getApplication().invokeLater(() -> showConflictActions(repoPath, unfinished));
                        return;
                    }

                    indicator.setText("Pulling latest changes from " + branch + "...");
                    commits.pull(repoPath, remoteUrl, remoteName, branch);

                    // Both directions, because the button says Sync. It used to
                    // pull and then report "Up to date with the remote" with the
                    // tester's own commits still sitting here - which is how a
                    // whole afternoon of work stayed on one machine while the
                    // message said it had not (#89).
                    indicator.setText("Pushing what is committed here...");
                    final int pushed = pushUnpushed(repoPath, remoteName, branch);

                    indicator.setText("Refreshing files...");
                    refreshAfterSync(repoPath, pushed);

                },
                ex -> {
                    Logger.error(ex.getMessage());

                    // Asked here, still on the background thread: answering it
                    // runs git status, and a git command on the EDT trips the
                    // platform's own assertion. The shared task hands the error
                    // to this handler on that thread for exactly this reason.
                    final boolean conflicts = git.hasConflicts(repoPath);

                    // Asked here too, for the same reason: naming the files
                    // that conflict is another git status.
                    final @NotNull List<String> conflicting = conflicts ? git.conflictingPaths(repoPath) : List.of();

                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (conflicts) {
                            showConflictActions(repoPath, conflicting);
                        } else {
                            reportSyncFailure("Could not sync with the remote:\n" + ex.getMessage());
                        }
                    });
                });
    }

    private void showConflictActions(final @NotNull Path repoPath, final @NotNull List<String> conflicting) {
        GitConflictOffer.show(p, conflicting,
                () -> resolveConflicts(repoPath),
                () -> finishRebase(repoPath, false),
                () -> finishRebase(repoPath, true));
    }

    /**
     * Merges the conflicted test cases and continues the pull when nothing is
     * left conflicting. Off the EDT: it reads Git and writes files.
     */
    private void resolveConflicts(final @NotNull Path repoPath) {
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ConflictResolution.resolveRebase(p, repoPath,
                        () -> finishSyncInBackground(repoPath),
                        leftOver -> showConflictActions(repoPath, leftOver)));
    }

    /**
     * Conflicts still in the way are not a failure the tester can read and act
     * on — they are the same situation that raised the conflict notification in
     * the first place, so it is raised again with its two buttons.
     */
    private void reportRebaseFailure(final @NotNull Path repoPath, final @NotNull String message) {
        // Called from a background task's body and from its error handler, both
        // off the EDT - which is where the git question has to be asked.
        final boolean conflicts = git.hasConflicts(repoPath);
        final @NotNull List<String> conflicting = conflicts ? git.conflictingPaths(repoPath) : List.of();

        ApplicationManager.getApplication().invokeLater(() -> {
            if (conflicts) showConflictActions(repoPath, conflicting);
            else Services.getInstance(p, Notifier.class).error(p, "Git Conflict Operation Failed", message);
        });
    }

    /**
     * Says the sync failed, with what went wrong under it.
     * <p>
     * One method rather than the title written out beside each detail: the two
     * handlers in this class both raise it, and a title spelled twice is a title
     * that stops agreeing the day one is reworded.
     */
    private void reportSyncFailure(final @NotNull String detail) {
        Services.getInstance(p, Notifier.class).error(p, "Sync Failed", detail);
    }

    private void finishRebase(final @NotNull Path repoPath, final boolean abort) {
        // One sentence for this attempt, whichever way it fails - the body's
        // refusal and the handler's both say it.
        final @NotNull String failure = abort ? "Could not abort the rebase." : "Could not continue the rebase.";

        GitBackgroundTask.run(p, abort ? "Aborting rebase" : "Continuing rebase", false,
                indicator -> {
                    // The reason is logged by the service; what is left here is
                    // the choice it cannot make - conflicts that remain are
                    // re-offered rather than reported as a plain failure (#63).
                    if (abort) {
                        if (git.couldNotAbortRebase(repoPath)) {
                            reportRebaseFailure(repoPath, failure);
                            return;
                        }
                        refreshRepository(repoPath);
                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).info(p, "Rebase aborted", "The pull was rolled back"));
                        return;
                    }

                    if (git.couldNotContinueRebase(repoPath)) {
                        reportRebaseFailure(repoPath, failure);
                        return;
                    }

                    // The same ending Resolve already took on this route: push
                    // what the rebase replayed, then refresh and re-index. It
                    // used to refresh without pushing, so a tester who continued
                    // had their colleague's cases on screen and their own still
                    // only on this machine, with nothing saying so.
                    finishSyncInBackground(repoPath);
                },
                // It had no error path at all. A throw in here left the tester
                // with a task that stopped and a stack trace in the log.
                ex -> {
                    Logger.error(ex.getMessage());
                    reportRebaseFailure(repoPath, failure);
                });
    }

    /**
     * Ends a sync that had stopped on conflicts: push what the rebase freed up,
     * then refresh.
     * <p>
     * In a background task because {@link ConflictResolution#resolveRebase}
     * hands back on the EDT, and everything here - pushing, refreshing the VFS
     * synchronously, rescanning the project - is work the EDT may not do. The
     * straight-through path got that for free by sitting inside a task already;
     * this one has to ask.
     */
    private void finishSyncInBackground(final @NotNull Path repoPath) {
        GitBackgroundTask.run(p, "Finishing sync", false,
                indicator -> {
                    // The same ending as a sync that never stopped: the commits
                    // the rebase just replayed are still only here, and a
                    // refresh that reported "up to date" over them would be the
                    // same untruth from the other door.
                    int pushed = 0;
                    try {
                        final @NotNull String remoteName = git.getRemoteName(repoPath);
                        if (!remoteName.isEmpty()) {
                            indicator.setText("Pushing what is committed here...");
                            pushed = pushUnpushed(repoPath, remoteName, git.syncBranch(repoPath));
                        }
                    } catch (final Exception ex) {
                        // Caught in here rather than left to the handler: the
                        // pull and the merge both worked, only the push did not,
                        // and the tree still has to be rebuilt around what
                        // arrived - so this is not the end of the work.
                        Logger.error("Could not push after resolving: " + ex.getMessage());
                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).error(p, "Push Failed",
                                        "The conflicts were resolved, but the push did not go through:\n" + ex.getMessage()));
                    }

                    indicator.setText("Refreshing files...");
                    refreshAfterSync(repoPath, pushed);
                },
                // The refresh had no error path. A throw there left the tester
                // with nothing said and a tree still showing what was there
                // before the sync.
                ex -> {
                    Logger.error(ex.getMessage());
                    ApplicationManager.getApplication().invokeLater(() ->
                            reportSyncFailure("The sync did not finish:\n" + ex.getMessage()));
                });
    }

    /**
     * Pushes the commits that are here and not on the remote, and answers how
     * many went.
     * <p>
     * Asked rather than attempted: a push with nothing to push still contacts
     * the remote, and on a sync pressed out of habit that is a network round
     * trip - and, over SSH, possibly a passphrase prompt - for no reason.
     */
    private int pushUnpushed(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch) {
        final int unpushed = git.unpushedCount(repoPath);
        if (unpushed == 0) return 0;

        commits.push(repoPath, remote, branch);
        return unpushed;
    }

    private void refreshAfterSync(final @NotNull Path repoPath, final int pushed) {
        RepositoryRefresh.after(p, repoPath);
        ApplicationManager.getApplication().invokeLater(() -> {
            // A balloon, not a log entry. A sync is pressed and watched: it
            // finishes in seconds with the tree rebuilding underneath it, and
            // what it leaves behind is the tree itself rather than a line in the
            // Notifications log. The failures still go there, which is what the
            // log is worth keeping for.
            Services.getInstance(p, Notifier.class).softShow(p, "Synced", pushed == 0
                    ? "Up to date with the remote"
                    : "Pushed " + pushed + (pushed == 1 ? " commit" : " commits"));
        });
    }

    private void refreshRepository(final @NotNull Path repoPath) {
        RepositoryRefresh.after(p, repoPath);
    }

    /**
     * The test project the selection sits under - the nearest one walking up the
     * selected path, and the tree's own root when nothing is selected.
     */
    private @NotNull Optional<Path> getActiveProjectPath() {
        return Optional.ofNullable(tree.getSelectionPath())
                .flatMap(SyncActionAction::projectOn)
                .or(() -> TreeValueUtil.projectPath(tree));
    }

    /**
     * The nearest test project on a selected path, walking down from the root.
     */
    private static @NotNull Optional<Path> projectOn(final @NotNull TreePath selectionPath) {
        for (final Object component : selectionPath.getPath()) {
            final @NotNull Optional<Path> project = TreeValueUtil.valueOf(component, TestProjectDirectoryDto.class)
                    .map(TestProjectDirectoryDto::getPath);
            if (project.isPresent()) return project;
        }

        return Optional.empty();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TreeValueUtil.selected(tree, TestProjectDirectoryDto.class).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
