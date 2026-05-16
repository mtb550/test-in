package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.util.git.GitDiffProcessor;
import org.testin.util.git.PendingCommitsDialog;
import org.testin.util.git.TestCaseDiff;
import org.testin.util.notifications.Notifier;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class ViewPendingCommits extends DumbAwareAction {

    private final SimpleTree tree;

    public ViewPendingCommits(SimpleTree tree) {
        super("View Pending Commits", "Review and push changed test cases", AllIcons.Actions.Commit);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Path repoPath = getActiveProjectPath();

        if (repoPath == null) {
            Notifier.getInstance().error("Git Error", "Could not determine the active project. Please select a project in the tree.");
            return;
        }

        File gitDir = new File(repoPath.toFile(), ".git");
        if (!gitDir.exists() || !gitDir.isDirectory()) {
            Notification notification = NotificationGroupManager.getInstance()
                    .getNotificationGroup("testin.notifications")
                    .createNotification(
                            "Git repository not found",
                            "The selected project (" + repoPath.getFileName() + ") is not a Git repository.",
                            NotificationType.WARNING
                    );

            notification.addAction(new NotificationAction("Initialize Git (git init)") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent event, @NotNull Notification notification) {
                    notification.expire();
                    initializeGitRepository(repoPath);
                }
            });

            notification.notify(Config.getProject());
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Scanning for changes", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    List<TestCaseDiff> changes = GitDiffProcessor.getPendingChanges(repoPath);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (changes.isEmpty()) {
                            Notifier.getInstance().info("No Changes", "Your test cases are up to date in this project.");
                            return;
                        }

                        PendingCommitsDialog dialog = new PendingCommitsDialog(Config.getProject(), changes, repoPath);
                        if (dialog.showAndGet()) {
                            String commitMessage = Messages.showInputDialog(
                                    Config.getProject(),
                                    "Enter a message for this commit:",
                                    "Commit Test Cases",
                                    Messages.getQuestionIcon(),
                                    "Updated test cases",
                                    null
                            );

                            if (commitMessage != null && !commitMessage.trim().isEmpty()) {
                                ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Committing to local Git", false) {
                                    @Override
                                    public void run(@NotNull ProgressIndicator commitIndicator) {
                                        commitIndicator.setIndeterminate(true);
                                        try {
                                            commitIndicator.setText("Staging files...");
                                            org.testin.util.git.GitCommandRunner.execute(repoPath, "git", "add", ".");

                                            commitIndicator.setText("Committing files...");
                                            org.testin.util.git.GitCommandRunner.execute(repoPath, "git", "commit", "-m", commitMessage);

                                            ApplicationManager.getApplication().invokeLater(() -> {
                                                Notification notification = NotificationGroupManager.getInstance()
                                                        .getNotificationGroup("testin.notifications")
                                                        .createNotification(
                                                                "Commit successful",
                                                                "Changes committed locally. Would you like to push to the remote repository now?",
                                                                NotificationType.INFORMATION
                                                        );

                                                notification.addAction(new NotificationAction("Push to Remote") {
                                                    @Override
                                                    public void actionPerformed(@NotNull AnActionEvent event, @NotNull Notification notification) {
                                                        notification.expire();
                                                        pushToRemote(repoPath);
                                                    }
                                                });

                                                notification.notify(Config.getProject());
                                            });

                                        } catch (Exception ex) {
                                            ApplicationManager.getApplication().invokeLater(() ->
                                                    Notifier.getInstance().error("Commit Failed", "Failed to commit changes:\n" + ex.getMessage())
                                            );
                                        }
                                    }
                                });
                            } else if (commitMessage != null) {
                                Notifier.getInstance().warn("Commit Aborted", "A commit message is required.");
                            }
                        }
                    });

                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().error("Git Error", "Failed to calculate diffs: " + ex.getMessage())
                    );
                }
            }
        });
    }

    private Path getActiveProjectPath() {
        TreePath selectionPath = tree.getSelectionPath();
        if (selectionPath != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            while (node != null) {
                if (node.getUserObject() instanceof TestProjectDirectoryDto prj) {
                    return prj.getPath();
                }
                node = (DefaultMutableTreeNode) node.getParent();
            }
        }

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        if (root != null && root.getUserObject() instanceof TestProjectDirectoryDto prj) {
            return prj.getPath();
        }

        return null;
    }

    private void initializeGitRepository(Path repoPath) {
        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Initializing git repository", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    org.testin.util.git.GitCommandRunner.execute(repoPath, "git", "init");

                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().info("Git Initialized", "Successfully initialized Git in:\n" + repoPath.getFileName())
                    );
                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().error("Git Init Failed", "Failed to initialize repository: " + ex.getMessage())
                    );
                }
            }
        });
    }

    private void pushToRemote(Path repoPath) {
        String remoteUrl = "";
        try {
            remoteUrl = org.testin.util.git.GitCommandRunner.execute(repoPath, "git", "config", "--get", "remote.origin.url").trim();
        } catch (Exception ignored) {
        }

        if (remoteUrl.isEmpty()) {
            remoteUrl = com.intellij.openapi.ui.Messages.showInputDialog(
                    Config.getProject(),
                    "No remote repository is configured for this project.\n\nPlease enter your Git Remote URL (e.g., https://github.com/user/repo.git):",
                    "Configure Remote",
                    com.intellij.openapi.ui.Messages.getQuestionIcon()
            );

            if (remoteUrl == null || remoteUrl.trim().isEmpty()) {
                Notifier.getInstance().warn("Push Aborted", "A remote URL is required to push.");
                return;
            }

            final String finalRemoteUrl = remoteUrl.trim();

            ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Configuring remote", false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        org.testin.util.git.GitCommandRunner.execute(repoPath, "git", "remote", "add", "origin", finalRemoteUrl);
                        executeGitPush(repoPath);
                    } catch (Exception ex) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Notifier.getInstance().error("Git Error", "Failed to add remote: " + ex.getMessage())
                        );
                    }
                }
            });
        } else {
            executeGitPush(repoPath);
        }
    }

    private void executeGitPush(Path repoPath) {
        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Pushing to Remote", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    indicator.setText("Syncing with remote (Pull --rebase)...");
                    try {
                        org.testin.util.git.GitCommandRunner.execute(repoPath, "git", "pull", "--rebase", "origin", "master");
                    } catch (Exception pullEx) {
                        try {
                            org.testin.util.git.GitCommandRunner.execute(repoPath, "git", "rebase", "--abort");
                        } catch (Exception ignored) {
                        }
                    }

                    indicator.setText("Pushing commits...");
                    org.testin.util.git.GitCommandRunner.execute(repoPath, "git", "push", "-u", "origin", "master");

                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().info("Push Successful", "Test cases were successfully pushed to the remote repository!")
                    );

                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().error("Push Failed", "Could not push to remote:\n" + ex.getMessage())
                    );
                }
            }
        });
    }
}