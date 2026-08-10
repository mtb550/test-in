package org.testin.git;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.projectPanel.tree.TreeValueUtil;
import org.testin.services.Services;
import org.testin.util.Tools;

import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.List;

// todo: to be refactored
public class ViewPendingCommitsAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull SimpleTree tree;
    private final @NotNull GitRepositoryService git;
    private final @NotNull GitCommitService commits;
    private Notification pushNotification;

    public ViewPendingCommitsAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super("View Pending Commits", "Review and push changed test cases", AllIcons.Actions.Commit);
        this.p = p;
        this.tree = tree;
        this.git = new GitRepositoryService(p);
        this.commits = new GitCommitService(p);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
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

        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Scanning for changes", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    List<TestCaseDiff> changes = GitDiffProcessor.getPendingChanges(p, path);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (changes.isEmpty()) {
                            Services.getInstance(p, Notifier.class).info(p, "No Changes", "Your test cases are up to date in this project.");
                            return;
                        }

                        PendingCommitsDialog dialog = new PendingCommitsDialog(p, changes, path);
                        if (dialog.showAndGet()) {
                            String commitMessage = Messages.showInputDialog(
                                    p,
                                    "Enter a message for this commit:",
                                    "Commit Test Cases",
                                    Messages.getQuestionIcon(),
                                    "Updated test cases",
                                    null
                            );

                            if (commitMessage != null && !commitMessage.trim().isEmpty()) {
                                performCommitWorkflow(p, path, commitMessage.trim());
                            } else if (commitMessage != null) {
                                Services.getInstance(p, Notifier.class).warn(p, "Commit Aborted", "A commit message is required.");
                            }
                        }
                    });

                } catch (final Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(p, Notifier.class).error(p, "Git Error", "Failed to calculate diffs: " + ex.getMessage())
                    );
                }
            }
        });
    }

    private void performCommitWorkflow(final @NotNull Project p, final Path repoPath, final String commitMessage) {
        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Committing to local Git", false) {
            @Override
            public void run(@NotNull ProgressIndicator commitIndicator) {
                commitIndicator.setIndeterminate(true);
                try {
                    commitIndicator.setText("Staging and committing files...");
                    commits.stageAndCommit(repoPath, commitMessage);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationAction pushAction = NotificationAction.createSimple(
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

                } catch (final Exception ex) {
                    String errorMsg = ex.getMessage();
                    if (isIdentityError(errorMsg)) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                promptAndSetGitIdentity(p, repoPath, commitMessage)
                        );
                    } else {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).error(p, "Commit Failed", "Failed to commit changes:\n" + errorMsg)
                        );
                    }
                }
            }
        });
    }

    private void initializeGitRepository(final @NotNull Project p, final Path repoPath) {
        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Initializing git repository", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    commits.initialize(repoPath);


                    ApplicationManager.getApplication().invokeLater(() -> Services.getInstance(p, Notifier.class).info(p, "Git Initialized", "Successfully initialized Git in:\n" + repoPath.getFileName()));

                } catch (final Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(p, Notifier.class).error(p, "Git Init Failed", "Failed to initialize repository: " + ex.getMessage())
                    );
                }
            }
        });
    }

    private void pushToRemote(final @NotNull Project p, final Path repoPath) {
        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Checking Git remote", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    final String remoteUrl = git.getRemoteUrl(repoPath);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (remoteUrl.isEmpty()) {
                            configureRemoteAndPush(p, repoPath);
                        } else {
                            executeGitPush(p, repoPath);
                        }
                    });
                } catch (final Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(p, Notifier.class).error(p, "Git Error", "Could not read the Git remote: " + ex.getMessage())
                    );
                }
            }
        });
    }

    private void configureRemoteAndPush(final @NotNull Project p, final Path repoPath) {
        final String remoteUrl = Messages.showInputDialog(
                p,
                "No remote repository is configured for this project.\n\nPlease enter your Git Remote URL (e.g., https://github.com/user/repo.git):",
                "Configure Remote",
                Messages.getQuestionIcon());

        if (remoteUrl == null || remoteUrl.trim().isEmpty()) {
            Services.getInstance(p, Notifier.class).warn(p, "Push Aborted", "A remote URL is required to push.");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Configuring remote", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    commits.configureRemote(repoPath, remoteUrl.trim());
                    ApplicationManager.getApplication().invokeLater(() -> executeGitPush(p, repoPath));
                } catch (final Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(p, Notifier.class).error(p, "Git Error", "Failed to add remote: " + ex.getMessage())
                    );
                }
            }
        });
    }

    private void executeGitPush(final @NotNull Project p, final Path repoPath) {
        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Pushing to Remote", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Syncing with remote (Pull --rebase)...");
                indicator.setText("Pushing commits...");
                commits.pullAndPushMain(repoPath);

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (pushNotification != null) {
                        pushNotification.expire();
                        pushNotification = null;
                    }

                    Services.getInstance(p, Notifier.class).info(p, "Push Successful", "Test cases were successfully pushed to the remote repository!");
                });
            }
        });
    }

    private void promptAndSetGitIdentity(final @NotNull Project p, final Path repoPath, final String pendingCommitMessage) {
        ApplicationManager.getApplication().invokeLater(() -> {
            GitIdentityDialog dialog = new GitIdentityDialog(p);

            if (dialog.showAndGet()) {
                String name = dialog.getUserName();
                String email = dialog.getUserEmail();
                boolean setGlobally = dialog.isSetGlobalConfig();

                if (name.trim().isEmpty() || email.trim().isEmpty()) {
                    Services.getInstance(p, Notifier.class).warn(p, "Missing Info", "Name and email are required to configure Git.");
                    return;
                }

                ProgressManager.getInstance().run(new Task.Backgroundable(p, "Configuring git identity", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setIndeterminate(true);
                        try {
                            commits.configureIdentity(repoPath, name.trim(), email.trim(), setGlobally);

                            ApplicationManager.getApplication().invokeLater(() -> {
                                Services.getInstance(p, Notifier.class).info(p, "Git Identity Set", "Identity configured successfully. Resuming commit...");
                                performCommitWorkflow(p, repoPath, pendingCommitMessage);
                            });
                        } catch (final Exception ex) {
                            ApplicationManager.getApplication().invokeLater(() ->
                                    Services.getInstance(p, Notifier.class).error(p, "Config Failed", "Failed to set Git identity:\n" + ex.getMessage())
                            );
                        }
                    }
                });
            }
        });
    }

    private boolean isIdentityError(final String message) {
        if (message == null) return false;
        final String normalized = message.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("author identity unknown")
                || normalized.contains("please tell me who you are");
    }

}
