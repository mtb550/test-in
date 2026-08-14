package org.testin.git;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.projectPanel.tree.TreeValueUtil;
import org.testin.services.Services;
import org.testin.util.Tools;

import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Review-and-push workflow for changed test cases: scan, review dialog, commit,
 * remote configuration, pull-rebase + push, and conflict handling. Every
 * background step runs through {@link GitBackgroundTask}.
 */
public class ViewPendingCommitsAction extends AbstractProjectTreeAction {
    private final @NotNull GitRepositoryService git;
    private final @NotNull GitCommitService commits;
    /**
     * Live only between a successful commit and the push that expires it.
     */
    private @Nullable Notification pushNotification;

    public ViewPendingCommitsAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, tree, "View Pending Commits", "Review and push changed test cases", AllIcons.Actions.Commit);
        this.git = new GitRepositoryService(p);
        this.commits = new GitCommitService(p);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();
        if (path == null) return;
        final Object userObject = TreeValueUtil.valueOf(path.getLastPathComponent());

        e.getPresentation().setEnabled(userObject instanceof TestProjectDirectoryDto);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        final Path path = Services.getInstance(p, Tools.class).getProjectPath(tree);
        if (path == null) return;

        if (!git.isRepository(path)) {
            Services.getInstance(p, Notifier.class).warnWithAction(p,
                    "Git repository not found",
                    "The selected project (" + path.getFileName() + ") is not a Git repository.",
                    "Initialize Git (git init)",
                    () -> initializeGitRepository(p, path)
            );

            return;
        }

        scanForChanges(p, path);
    }

    private void scanForChanges(final @NotNull Project p, final @NotNull Path path) {
        GitBackgroundTask.run(p, "Scanning for changes", true,
                indicator -> {
                    final List<TestCaseDiff> changes = GitDiffProcessor.getPendingChanges(p, path);
                    ApplicationManager.getApplication().invokeLater(() -> reviewChanges(p, path, changes));
                },
                ex -> Services.getInstance(p, Notifier.class).error(p, "Git Error", "Failed to calculate diffs: " + ex.getMessage()));
    }

    private void reviewChanges(final @NotNull Project p, final @NotNull Path path,
                               final @NotNull List<TestCaseDiff> changes) {
        if (changes.isEmpty()) {
            Services.getInstance(p, Notifier.class).info(p, "No Changes", "Your test cases are up to date in this project.");
            return;
        }

        final PendingCommitsDialog dialog = new PendingCommitsDialog(p, changes, path);
        if (!dialog.showAndGet()) return;

        final List<TestCaseDiff> selectedChanges = dialog.getSelectedDifferences();
        if (selectedChanges.isEmpty()) {
            Services.getInstance(p, Notifier.class).warn(p, "Commit Aborted", "Select at least one change to commit.");
            return;
        }

        final String commitMessage = Messages.showInputDialog(
                p,
                "Enter a message for this commit:",
                "Commit Test Cases",
                Messages.getQuestionIcon(),
                "Updated test cases",
                null
        );

        if (commitMessage != null && !commitMessage.trim().isEmpty()) {
            performCommitWorkflow(p, path, commitMessage.trim(), selectedChanges);
        } else if (commitMessage != null) {
            Services.getInstance(p, Notifier.class).warn(p, "Commit Aborted", "A commit message is required.");
        }
    }

    private void performCommitWorkflow(
            final @NotNull Project p,
            final @NotNull Path repoPath,
            final @NotNull String commitMessage,
            final @NotNull Collection<TestCaseDiff> selectedChanges) {
        GitBackgroundTask.run(p, "Committing to local Git", false,
                indicator -> {
                    indicator.setText("Staging and committing files..");
                    commits.stageAndCommit(repoPath, commitMessage, selectedChanges);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        final NotificationAction pushAction = NotificationAction.createSimple(
                                "Push to Remote",
                                () -> pushToRemote(p, repoPath)
                        );

                        pushNotification = Services.getInstance(p, Notifier.class).infoWithActions(
                                p,
                                "Commit successful",
                                "Changes committed locally. Would you like to push to the remote repository now?",
                                pushAction
                        );
                    });
                },
                ex -> {
                    if (isIdentityError(ex.getMessage())) {
                        promptAndSetGitIdentity(p, repoPath, commitMessage, selectedChanges);
                    } else {
                        Services.getInstance(p, Notifier.class).error(p, "Commit Failed", "Failed to commit changes:\n" + ex.getMessage());
                    }
                });
    }

    private void initializeGitRepository(final @NotNull Project p, final @NotNull Path repoPath) {
        GitBackgroundTask.run(p, "Initializing git repository", false,
                indicator -> {
                    commits.initialize(repoPath);
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(p, Notifier.class).info(p, "Git Initialized", "Successfully initialized Git in:\n" + repoPath.getFileName()));
                },
                ex -> Services.getInstance(p, Notifier.class).error(p, "Git Init Failed", "Failed to initialize repository: " + ex.getMessage()));
    }

    private void pushToRemote(final @NotNull Project p, final @NotNull Path repoPath) {
        GitBackgroundTask.run(p, "Checking Git remote", false,
                indicator -> {
                    final String remoteName = git.getRemoteName(repoPath);
                    final String remoteUrl = remoteName == null ? "" : git.getRemoteUrl(repoPath, remoteName);
                    final String branch = git.getDefaultBranch(repoPath);
                    if (branch == null || branch.isBlank()) {
                        throw new IllegalStateException("Could not determine the repository default branch.");
                    }
                    ApplicationManager.getApplication().invokeLater(() -> {
                        // A null remote name always yields an empty URL above; naming it here
                        // keeps the "already configured" branch provably non-null.
                        if (remoteName == null || remoteUrl.isEmpty()) {
                            configureRemoteAndPush(p, repoPath, remoteName == null ? "origin" : remoteName, branch);
                        } else {
                            executeGitPush(p, repoPath, remoteName, branch);
                        }
                    });
                },
                ex -> Services.getInstance(p, Notifier.class).error(p, "Git Error", "Could not read the Git remote: " + ex.getMessage()));
    }

    private void configureRemoteAndPush(final @NotNull Project p, final @NotNull Path repoPath,
                                        final @NotNull String remoteName, final @NotNull String branch) {
        final String remoteUrl = Messages.showInputDialog(
                p,
                "No remote repository is configured for this project.\n\nPlease enter your Git Remote URL (e.g., https://github.com/user/repo.git):",
                "Configure Remote",
                Messages.getQuestionIcon());

        if (remoteUrl == null || remoteUrl.trim().isEmpty()) {
            Services.getInstance(p, Notifier.class).warn(p, "Push Aborted", "A remote URL is required to push.");
            return;
        }

        GitBackgroundTask.run(p, "Configuring remote", false,
                indicator -> {
                    commits.configureRemote(repoPath, remoteName, remoteUrl.trim());
                    ApplicationManager.getApplication().invokeLater(() -> executeGitPush(p, repoPath, remoteName, branch));
                },
                ex -> Services.getInstance(p, Notifier.class).error(p, "Git Error", "Failed to add remote: " + ex.getMessage()));
    }

    private void executeGitPush(final @NotNull Project p, final @NotNull Path repoPath,
                                final @NotNull String remote, final @NotNull String branch) {
        GitBackgroundTask.run(p, "Pushing to Remote", false,
                indicator -> {
                    indicator.setText("Syncing with remote (pull --rebase, then push)..");
                    commits.pullAndPush(repoPath, remote, branch);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (pushNotification != null) {
                            pushNotification.expire();
                            pushNotification = null;
                        }
                        Services.getInstance(p, Notifier.class).info(p, "Push Successful", "Test cases were successfully pushed to the remote repository!");
                    });
                },
                ex -> {
                    if (git.hasConflicts(repoPath)) showConflictActions(repoPath, remote, branch);
                    else Services.getInstance(p, Notifier.class).error(p, "Push Failed", ex.getMessage());
                });
    }

    private void showConflictActions(final @NotNull Path repoPath, final @NotNull String remote,
                                     final @NotNull String branch) {
        Services.getInstance(p, Notifier.class).warnWithActions(
                p,
                "Git Conflicts",
                "Pull stopped because conflicts must be resolved in the IDE before continuing.",
                NotificationAction.createSimple("Continue rebase", () -> finishRebase(repoPath, remote, branch, false)),
                NotificationAction.createSimple("Abort rebase", () -> finishRebase(repoPath, remote, branch, true)));
    }

    private void finishRebase(final @NotNull Path repoPath, final @NotNull String remote,
                              final @NotNull String branch, final boolean abort) {
        GitBackgroundTask.run(p, abort ? "Aborting rebase" : "Continuing rebase", false,
                indicator -> {
                    if (abort) {
                        git.abortRebase(repoPath);
                    } else {
                        git.continueRebase(repoPath);
                        commits.push(repoPath, remote, branch);
                    }
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(p, Notifier.class).info(p, "Git Conflict Resolution", abort ? "Rebase aborted." : "Rebase continued and changes were pushed."));
                },
                ex -> {
                    if (git.hasConflicts(repoPath)) showConflictActions(repoPath, remote, branch);
                    else
                        Services.getInstance(p, Notifier.class).error(p, "Git Conflict Operation Failed", ex.getMessage());
                });
    }

    private void promptAndSetGitIdentity(
            final @NotNull Project p,
            final @NotNull Path repoPath,
            final @NotNull String pendingCommitMessage,
            final @NotNull Collection<TestCaseDiff> selectedChanges) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final GitIdentityDialog dialog = new GitIdentityDialog(p);
            if (!dialog.showAndGet()) return;

            final String name = dialog.getUserName();
            final String email = dialog.getUserEmail();
            final boolean setGlobally = dialog.isSetGlobalConfig();

            if (name.trim().isEmpty() || email.trim().isEmpty()) {
                Services.getInstance(p, Notifier.class).warn(p, "Missing Info", "Name and email are required to configure Git.");
                return;
            }

            GitBackgroundTask.run(p, "Configuring git identity", false,
                    indicator -> {
                        commits.configureIdentity(repoPath, name.trim(), email.trim(), setGlobally);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            Services.getInstance(p, Notifier.class).info(p, "Git Identity Set", "Identity configured successfully. Resuming commit..");
                            performCommitWorkflow(p, repoPath, pendingCommitMessage, selectedChanges);
                        });
                    },
                    ex -> Services.getInstance(p, Notifier.class).error(p, "Config Failed", "Failed to set Git identity:\n" + ex.getMessage()));
        });
    }

    private boolean isIdentityError(final @Nullable String message) {
        if (message == null) return false;

        final String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("author identity unknown")
                || normalized.contains("please tell me who you are");
    }
}
