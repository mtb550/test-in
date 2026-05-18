package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import git4idea.checkin.GitUserNameNotDefinedDialog;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.util.Tools;
import org.testin.util.git.GitCommandRunner;
import org.testin.util.git.GitDiffProcessor;
import org.testin.util.git.PendingCommitsDialog;
import org.testin.util.git.TestCaseDiff;
import org.testin.util.notifications.Notifier;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class ViewPendingCommits extends DumbAwareAction {
    private final SimpleTree tree;

    public ViewPendingCommits(final @NotNull SimpleTree tree) {
        super("View Pending Commits", "Review and push changed test cases", AllIcons.Actions.Commit);
        this.tree = tree;
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();
        if (path == null) return;
        final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        final Object userObject = selectedNode.getUserObject();

        e.getPresentation().setEnabled(userObject instanceof TestProjectDirectoryDto);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Path path = Tools.getInstance().getProjectPath(tree);

        File gitDir = new File(path.toFile(), ".git");
        if (!gitDir.exists() || !gitDir.isDirectory()) {
            Notifier.getInstance().warnWithAction(
                    "Git repository not found",
                    "The selected project (" + path.getFileName() + ") is not a Git repository.",
                    "Initialize Git (git init)",
                    () -> initializeGitRepository(path)
            );

            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Scanning for changes", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    List<TestCaseDiff> changes = GitDiffProcessor.getPendingChanges(path);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (changes.isEmpty()) {
                            Notifier.getInstance().info("No Changes", "Your test cases are up to date in this project.");
                            return;
                        }

                        PendingCommitsDialog dialog = new PendingCommitsDialog(Config.getProject(), changes, path);
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
                                performCommitWorkflow(path, commitMessage.trim());
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

    private void performCommitWorkflow(Path repoPath, String commitMessage) {
        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Committing to local Git", false) {
            @Override
            public void run(@NotNull ProgressIndicator commitIndicator) {
                commitIndicator.setIndeterminate(true);
                try {
                    commitIndicator.setText("Staging files...");
                    GitCommandRunner.execute(repoPath, "git", "add", ".");

                    commitIndicator.setText("Committing files...");
                    GitCommandRunner.execute(repoPath, "git", "commit", "-m", commitMessage);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        Notifier.getInstance().infoWithAction(
                                "Commit successful",
                                "Changes committed locally. Would you like to push to the remote repository now?",
                                "Push to Remote",
                                () -> pushToRemote(repoPath)
                        );
                    });

                } catch (Exception ex) {
                    String errorMsg = ex.getMessage();
                    if (errorMsg != null && errorMsg.contains("Author identity unknown")) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                promptAndSetGitIdentity(repoPath, commitMessage)
                        );
                    } else {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Notifier.getInstance().error("Commit Failed", "Failed to commit changes:\n" + errorMsg)
                        );
                    }
                }
            }
        });
    }

    private void initializeGitRepository(Path repoPath) {
        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Initializing git repository", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    GitCommandRunner.execute(repoPath, "git", "init");

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
            remoteUrl = GitCommandRunner.execute(repoPath, "git", "config", "--get", "remote.origin.url").trim();
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
                        GitCommandRunner.execute(repoPath, "git", "remote", "add", "origin", finalRemoteUrl);
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
                        GitCommandRunner.execute(repoPath, "git", "pull", "--rebase", "--autostash", "origin", "main");
                    } catch (Exception pullEx) {
                        try {
                            GitCommandRunner.execute(repoPath, "git", "rebase", "--abort");
                        } catch (Exception ignored) {
                        }
                    }

                    indicator.setText("Pushing commits...");
                    GitCommandRunner.execute(repoPath, "git", "push", "-u", "origin", "main");

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

    private void promptAndSetGitIdentity(Path repoPath, String pendingCommitMessage) {
        ApplicationManager.getApplication().invokeLater(() -> {

            VirtualFile vRepoPath = LocalFileSystem.getInstance().findFileByIoFile(repoPath.toFile());
            if (vRepoPath == null) return;

            GitUserNameNotDefinedDialog dialog = new GitUserNameNotDefinedDialog(
                    Config.getProject(),
                    Collections.singletonList(vRepoPath),
                    Collections.singletonList(vRepoPath),
                    Collections.emptyMap(),
                    "Set Identity and Commit"
            );

            if (dialog.showAndGet()) {
                String name = dialog.getUserName();
                String email = dialog.getUserEmail();
                boolean setGlobally = dialog.isSetGlobalConfig();

                if (name.trim().isEmpty() || email.trim().isEmpty()) {
                    Notifier.getInstance().warn("Missing Info", "Name and email are required to configure Git.");
                    return;
                }

                ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Configuring git identity", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setIndeterminate(true);
                        try {
                            String scope = setGlobally ? "--global" : "--local";
                            GitCommandRunner.execute(repoPath, "git", "config", scope, "user.name", name.trim());
                            GitCommandRunner.execute(repoPath, "git", "config", scope, "user.email", email.trim());

                            ApplicationManager.getApplication().invokeLater(() -> {
                                Notifier.getInstance().info("Git Identity Set", "Identity configured successfully. Resuming commit...");
                                performCommitWorkflow(repoPath, pendingCommitMessage);
                            });
                        } catch (Exception ex) {
                            ApplicationManager.getApplication().invokeLater(() ->
                                    Notifier.getInstance().error("Config Failed", "Failed to set Git identity:\n" + ex.getMessage())
                            );
                        }
                    }
                });
            }
        });
    }
}