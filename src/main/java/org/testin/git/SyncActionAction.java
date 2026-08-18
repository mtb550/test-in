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
import org.jetbrains.annotations.Nullable;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.explorer.ExplorerPanel;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import javax.swing.tree.TreePath;
import java.nio.file.Path;

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

        final Path repoPath = getActiveProjectPath();

        if (repoPath == null) {
            Services.getInstance(p, Notifier.class).error(p, "Sync Error", "Could not determine the active project. Please select a project in the tree.");
            return;
        }

        if (!git.isRepository(repoPath)) {
            Services.getInstance(p, Notifier.class).warn(p, "Sync Error", "This project is not a Git repository. Initialize it first.");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Syncing with remote", true) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                try {
                    indicator.setText("Checking remote configuration..");
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

                    indicator.setText("Pulling latest changes from " + branch + "..");
                    sync.pull(repoPath, remoteName, branch);

                    indicator.setText("Refreshing files..");
                    refreshAfterSync(repoPath);

                } catch (final Exception ex) {
                    Logger.error(ex.getMessage());
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (sync.hasConflicts(repoPath)) {
                            showConflictActions(repoPath);
                        } else {
                            Services.getInstance(p, Notifier.class).error(p, "Sync Failed", "Could not pull changes:\n" + ex.getMessage());
                        }
                    });
                }
            }
        });
    }

    private void showConflictActions(final @NotNull Path repoPath) {
        final NotificationAction continueAction = NotificationAction.createSimple(
                "Continue rebase", () -> finishRebase(repoPath, false));
        final NotificationAction abortAction = NotificationAction.createSimple(
                "Abort rebase", () -> finishRebase(repoPath, true));
        Services.getInstance(p, Notifier.class).warnWithActions(
                p,
                "Git Conflicts",
                "Pull stopped because conflicts must be resolved in the IDE before continuing.",
                continueAction,
                abortAction);
    }

    /**
     * Conflicts still in the way are not a failure the tester can read and act
     * on — they are the same situation that raised the conflict notification in
     * the first place, so it is raised again with its two buttons.
     */
    private void reportRebaseFailure(final @NotNull Path repoPath, final @NotNull String message) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (sync.hasConflicts(repoPath)) showConflictActions(repoPath);
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
                    if (!sync.abortRebase(repoPath)) {
                        reportRebaseFailure(repoPath, "Could not abort the rebase.");
                        return;
                    }
                    refreshRepository(repoPath);
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(p, Notifier.class).info(p, "Rebase aborted", "The pull was rolled back"));
                    return;
                }

                if (!sync.continueRebase(repoPath)) {
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
            // A real notification, not a soft balloon: a sync runs in the
            // background and can finish while the tester is in another window,
            // so it has to survive in the log rather than fade (#62).
            Services.getInstance(p, Notifier.class).info(p, "Synced", "Up to date with the remote");
            pp.getProjectTree().refresh();
        });
    }

    private void refreshRepository(final @NotNull Path repoPath) {
        final VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(repoPath.toFile());
        if (vFile != null) GitUtil.refreshVfsInRoot(vFile);
        Services.getInstance(p, ProjectIndexer.class).scanSingleProject(repoPath);
    }

    private @Nullable Path getActiveProjectPath() {
        final TreePath selectionPath = tree.getSelectionPath();
        if (selectionPath != null) {
            for (final Object component : selectionPath.getPath()) {
                final TestProjectDirectoryDto project = TreeValueUtil.valueOf(component, TestProjectDirectoryDto.class);
                if (project != null) return project.getPath();
            }
        }

        final TestProjectDirectoryDto root = TreeValueUtil.valueOf(tree.getModel().getRoot(), TestProjectDirectoryDto.class);
        return root == null ? null : root.getPath();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();
        if (path == null) return;
        e.getPresentation().setEnabled(TreeValueUtil.valueOf(path.getLastPathComponent(), TestProjectDirectoryDto.class) != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
