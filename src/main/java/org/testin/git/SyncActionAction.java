package org.testin.git;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.util.Bundle;

import javax.swing.tree.TreePath;
import java.util.Optional;
import java.nio.file.Path;
import java.util.List;

/**
 * Pulls what the remote has, then pushes what this machine has committed.
 * <p>
 * Declared in {@code plugin.xml} (#119), so Find Action offers it and a tester
 * can bind a key to it - it has never had one. In the main descriptor rather
 * than the Git one, for the reason the menu entry is added in every IDE: an
 * action that vanishes where Git is missing teaches nobody the feature exists,
 * so it is present and grayed with the reason on it (#273).
 * <p>
 * No constructor and no fields: the platform builds one instance for the whole
 * IDE, so the repository comes from the keystroke and the two Git services -
 * each of which is built around one project - belong to {@link Work}.
 */
public class SyncActionAction extends DumbAwareAction {

    // UC-SHARE-016
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        activeProjectPath(e).ifPresentOrElse(path -> new Work(p).syncRepository(path), () ->
                Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.sync.error.title"),
                        Bundle.message("git.sync.no.project")));
    }

    // UC-SHARE-016
    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Rule-SHARE-105. Grayed with the reason in it when Git is missing,
        // rather than left out of the menu entirely (#273).
        if (!OptionalPlugin.GIT.enableOrExplain(this, e.getPresentation())) return;

        e.getPresentation().setEnabled(TestinData.firstSelected(e, TestProjectDirectoryDto.class).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    /**
     * The test project the selection sits under - the nearest one walking up the
     * selected path, and the tree's own root when nothing is selected. Empty
     * when the keystroke never arrived in the Testin tree.
     */
    private static @NotNull Optional<Path> activeProjectPath(final @NotNull AnActionEvent e) {
        return TestinData.tree(e).flatMap(SyncActionAction::activeProjectIn);
    }

    private static @NotNull Optional<Path> activeProjectIn(final @NotNull SimpleTree tree) {
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

    /**
     * Syncing one repository, for a project that is there.
     *
     * @param git     what a repository can be asked
     * @param commits the pull and the push, through the same service the review
     *                pushes through - so there is one way commits leave this
     *                machine rather than two
     */
    private record Work(@NotNull Project p, @NotNull GitRepositoryService git, @NotNull GitCommitService commits) {

        private Work(final @NotNull Project p) {
            this(p, new GitRepositoryService(p), new GitCommitService(p));
        }

        /**
         * UC-SHARE-016, Rule-SHARE-069.
         * <p>
         * Everything the action does once it knows which repository it is syncing.
         */
        private void syncRepository(final @NotNull Path repoPath) {

            // Soft, and not an error: nothing failed. The tester pressed Sync on a
            // test project that was never put under Git, and the sentence says which
            // project and where the repository comes from - the review is what
            // offers to create one.
            if (git.isNotRepository(repoPath)) {
                Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("git.sync.nothing.title"),
                        Bundle.message("git.sync.nothing.message", repoPath.getFileName()));
                return;
            }

            GitBackgroundTask.run(p, Bundle.message("git.task.syncing"), true,
                    indicator -> {
                        indicator.setText(Bundle.message("git.progress.checking.remote"));
                        final @NotNull String remoteName = git.getRemoteName(repoPath);
                        final @NotNull String remoteUrl = remoteName.isEmpty() ? "" : git.getRemoteUrl(repoPath, remoteName);

                        if (remoteUrl.isEmpty()) {
                            ApplicationManager.getApplication().invokeLater(() ->
                                    Services.getInstance(p, Notifier.class).warn(p, Bundle.message("git.sync.aborted.title"), Bundle.message("git.sync.aborted.message"))
                            );
                            return;
                        }

                        final @NotNull String branch = git.syncBranch(repoPath);
                        if (branch.isBlank()) {
                            throw new IllegalStateException(Bundle.message("git.error.no.sync.branch"));
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

                        indicator.setText(Bundle.message("git.progress.pulling", branch));
                        commits.pull(repoPath, remoteUrl, remoteName, branch);

                        // Both directions, because the button says Sync. It used to
                        // pull and then report "Up to date with the remote" with the
                        // tester's own commits still sitting here - which is how a
                        // whole afternoon of work stayed on one machine while the
                        // message said it had not (#89).
                        indicator.setText(Bundle.message("git.progress.pushing.committed"));
                        final int pushed = pushUnpushed(repoPath, remoteName, branch);

                        indicator.setText(Bundle.message("git.progress.refreshing"));
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
                                reportSyncFailure(Bundle.message("git.sync.failed.remote", ex.getMessage()));
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
         * UC-SHARE-017.
         * <p>
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
                else Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.conflict.operation.failed.title"), message);
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
            Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.sync.failed.title"), detail);
        }

        // UC-SHARE-017, Rule-SHARE-077
        private void finishRebase(final @NotNull Path repoPath, final boolean abort) {
            // One sentence for this attempt, whichever way it fails - the body's
            // refusal and the handler's both say it.
            final @NotNull String failure = abort ? Bundle.message("git.error.abort.rebase") : Bundle.message("git.error.continue.rebase");

            GitBackgroundTask.run(p, abort ? Bundle.message("git.task.aborting.rebase") : Bundle.message("git.task.continuing.rebase"), false,
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
                                    Services.getInstance(p, Notifier.class).info(p, Bundle.message("git.rebase.aborted.title"), Bundle.message("git.rebase.aborted.pull.message")));
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
            GitBackgroundTask.run(p, Bundle.message("git.task.finishing.sync"), false,
                    indicator -> {
                        // The same ending as a sync that never stopped: the commits
                        // the rebase just replayed are still only here, and a
                        // refresh that reported "up to date" over them would be the
                        // same untruth from the other door.
                        int pushed = 0;
                        try {
                            final @NotNull String remoteName = git.getRemoteName(repoPath);
                            if (!remoteName.isEmpty()) {
                                indicator.setText(Bundle.message("git.progress.pushing.committed"));
                                pushed = pushUnpushed(repoPath, remoteName, git.syncBranch(repoPath));
                            }
                        } catch (final Exception ex) {
                            // Caught in here rather than left to the handler: the
                            // pull and the merge both worked, only the push did not,
                            // and the tree still has to be rebuilt around what
                            // arrived - so this is not the end of the work.
                            Logger.error("Could not push after resolving: " + ex.getMessage());
                            ApplicationManager.getApplication().invokeLater(() ->
                                    Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.push.failed.title"),
                                            Bundle.message("git.push.failed.after.resolve", ex.getMessage())));
                        }

                        indicator.setText(Bundle.message("git.progress.refreshing"));
                        refreshAfterSync(repoPath, pushed);
                    },
                    // The refresh had no error path. A throw there left the tester
                    // with nothing said and a tree still showing what was there
                    // before the sync.
                    ex -> {
                        Logger.error(ex.getMessage());
                        ApplicationManager.getApplication().invokeLater(() ->
                                reportSyncFailure(Bundle.message("git.sync.did.not.finish", ex.getMessage())));
                    });
        }

        /**
         * UC-SHARE-016, Rule-SHARE-070.
         * <p>
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

        // UC-SHARE-016
        private void refreshAfterSync(final @NotNull Path repoPath, final int pushed) {
            RepositoryRefresh.after(p, repoPath);
            ApplicationManager.getApplication().invokeLater(() -> {
                // A balloon, not a log entry. A sync is pressed and watched: it
                // It stays in the Notifications log, like the push beside it and
                // the server sync beside that. This used to fade, on the argument
                // that the rebuilt tree is what it leaves behind - but a sync runs
                // in the background and lands on its own time, so a tester reading a
                // bug report while it finishes had no way to learn it had (#268).
                // CLAUDE.md draws that line: work that completes while nobody is
                // looking is the work that must still be there afterwards.
                Services.getInstance(p, Notifier.class).info(p, Bundle.message("git.synced.title"), pushedMessage(pushed));
            });
        }

        /**
         * UC-SHARE-016, Rule-SHARE-070.
         * <p>
         * What a finished sync says: up to date, or how many commits went.
         * <p>
         * Three sentences rather than one built from pieces, because the count
         * and the word for it do not sit in the same order in every language -
         * and the count is passed as digits, so it reads 1234 rather than 1,234.
         */
        private static @NotNull String pushedMessage(final int pushed) {
            if (pushed == 0) return Bundle.message("git.synced.up.to.date");

            return pushed == 1
                    ? Bundle.message("git.synced.pushed.one")
                    : Bundle.message("git.synced.pushed.many", String.valueOf(pushed));
        }

        private void refreshRepository(final @NotNull Path repoPath) {
            RepositoryRefresh.after(p, repoPath);
        }
    }
}
