package org.testin.git;

import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import git4idea.GitUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.explorer.ExplorerPanel;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import javax.swing.tree.TreePath;
import java.util.Optional;
import java.nio.file.Path;
import java.util.List;

public class SyncActionAction extends AbstractProjectTreeAction {
    private final @NotNull ExplorerPanel pp;
    private final @NotNull GitRepositoryService git;
    private final @NotNull GitSyncService sync;

    public SyncActionAction(final @NotNull Project p, final @NotNull SimpleTree tree, final @NotNull ExplorerPanel pp) {
        super(p, tree, "Sync / Pull Changes", "Pull the latest test cases from the remote repository", AllIcons.Actions.SyncPanels);
        this.pp = pp;
        this.git = new GitRepositoryService(p);
        this.sync = new GitSyncService(p);
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
        if (!git.isRepository(repoPath)) {
            Services.getInstance(p, Notifier.class).softShow(p, "Nothing to Sync",
                    "'" + repoPath.getFileName() + "' is not under Git yet. Open Pending Commits to create the repository.");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Syncing with remote", true) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                try {
                    indicator.setText("Checking remote configuration...");
                    final String remoteName = git.getRemoteName(repoPath);
                    final String remoteUrl = remoteName == null ? "" : git.getRemoteUrl(repoPath, remoteName);

                    if (remoteName == null || remoteUrl.isEmpty()) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).warn(p, "Sync Aborted", "No remote URL is configured for this project. Push a commit first to configure the remote.")
                        );
                        return;
                    }

                    final String branch = git.getDefaultBranch(repoPath);
                    if (branch == null || branch.isBlank()) {
                        throw new IllegalStateException("Could not determine the repository default branch.");
                    }

                    indicator.setText("Pulling latest changes from " + branch + "...");
                    sync.pull(repoPath, remoteName, branch);

                    indicator.setText("Refreshing files...");
                    refreshAfterSync(repoPath);

                } catch (final Exception ex) {
                    Logger.error(ex.getMessage());

                    // Asked here, still on the background thread: answering it
                    // runs git status, and a git command on the EDT trips the
                    // platform's own assertion.
                    final boolean conflicts = git.hasConflicts(repoPath);

                    // Asked here too, for the same reason: naming the files
                    // that conflict is another git status.
                    final List<String> conflicting = conflicts ? git.conflictingPaths(repoPath) : List.of();

                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (conflicts) {
                            showConflictActions(repoPath, conflicting);
                        } else {
                            Services.getInstance(p, Notifier.class).error(p, "Sync Failed", "Could not pull changes:\n" + ex.getMessage());
                        }
                    });
                }
            }
        });
    }

    private void showConflictActions(final @NotNull Path repoPath, final @NotNull List<String> conflicting) {
        final Notifier notifier = Services.getInstance(p, Notifier.class);

        final NotificationAction resolveAction = notifier.action(
                "Resolve", () -> resolveConflicts(repoPath, conflicting));
        final NotificationAction continueAction = notifier.action(
                "Continue rebase", () -> finishRebase(repoPath, false));
        final NotificationAction abortAction = notifier.action(
                "Abort rebase", () -> finishRebase(repoPath, true));

        notifier.warnWithActions(
                p,
                "Git Conflicts",
                GitRefs.conflictMessage(conflicting),
                resolveAction,
                continueAction,
                abortAction);
    }

    /**
     * Merges the conflicted test cases and continues the pull when nothing is
     * left conflicting. Off the EDT: it reads Git and writes files.
     */
    private void resolveConflicts(final @NotNull Path repoPath, final @NotNull List<String> conflicting) {
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ConflictResolution.resolve(p, repoPath, conflicting,
                        () -> finishRebase(repoPath, false),
                        leftOver -> Services.getInstance(p, Notifier.class).warn(p, "Still Conflicting",
                                GitRefs.conflictMessage(leftOver))));
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
        final List<String> conflicting = conflicts ? git.conflictingPaths(repoPath) : List.of();

        ApplicationManager.getApplication().invokeLater(() -> {
            if (conflicts) showConflictActions(repoPath, conflicting);
            else Services.getInstance(p, Notifier.class).error(p, "Git Conflict Operation Failed", message);
        });
    }

    private void finishRebase(final @NotNull Path repoPath, final boolean abort) {
        ProgressManager.getInstance().run(new Task.Backgroundable(p, abort ? "Aborting rebase" : "Continuing rebase", false) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                // The reason is logged by the service; what is left here is the
                // choice it cannot make - conflicts that remain are re-offered
                // rather than reported as a plain failure (#63).
                if (abort) {
                    if (git.couldNotAbortRebase(repoPath)) {
                        reportRebaseFailure(repoPath, "Could not abort the rebase.");
                        return;
                    }
                    refreshRepository(repoPath);
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(p, Notifier.class).info(p, "Rebase aborted", "The pull was rolled back"));
                    return;
                }

                if (git.couldNotContinueRebase(repoPath)) {
                    reportRebaseFailure(repoPath, "Could not continue the rebase.");
                    return;
                }
                refreshAfterSync(repoPath);
            }
        });
    }

    private void refreshAfterSync(final @NotNull Path repoPath) {
        refreshRepository(repoPath);
        ApplicationManager.getApplication().invokeLater(() -> {
            // A balloon, not a log entry. A sync is pressed and watched: it
            // finishes in seconds with the tree rebuilding underneath it, and
            // what it leaves behind is the tree itself rather than a line in the
            // Notifications log. The failures still go there, which is what the
            // log is worth keeping for.
            Services.getInstance(p, Notifier.class).softShow(p, "Synced", "Up to date with the remote");
            pp.getProjectTree().refresh();
        });
    }

    private void refreshRepository(final @NotNull Path repoPath) {
        final VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(repoPath.toFile());
        if (vFile != null) GitUtil.refreshVfsInRoot(vFile);
        Services.getInstance(p, ProjectIndexer.class).scanSingleProject(repoPath);
    }

    /**
     * The test project the selection sits under - the nearest one walking up the
     * selected path, and the tree's own root when nothing is selected.
     */
    private @NotNull Optional<Path> getActiveProjectPath() {
        final TreePath selectionPath = tree.getSelectionPath();
        if (selectionPath != null) {
            for (final Object component : selectionPath.getPath()) {
                final Optional<Path> project = TreeValueUtil.valueOf(component, TestProjectDirectoryDto.class)
                        .map(TestProjectDirectoryDto::getPath);
                if (project.isPresent()) return project;
            }
        }

        return TreeValueUtil.projectPath(tree);
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
