package org.testin;

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
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.util.*;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

// todo: to be refactored
public class ViewPendingCommitsAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull SimpleTree tree;
    private Notification pushNotification;

    public ViewPendingCommitsAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super("View Pending Commits", "Review and push changed test cases", AllIcons.Actions.Commit);
        this.p = p;
        this.tree = tree;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
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

        final Path path = Services.getInstance(p, Tools.class).getProjectPath(tree);
        if (path == null) return;

        File gitDir = new File(path.toFile(), ".git");
        if (!gitDir.exists() || !gitDir.isDirectory()) {
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
                    commitIndicator.setText("Staging files...");
                    GitCommandRunner.execute(repoPath, "git", "add", ".");

                    commitIndicator.setText("Committing files...");
                    GitCommandRunner.execute(repoPath, "git", "commit", "-m", commitMessage);

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
                    if (errorMsg != null && errorMsg.contains("Author identity unknown")) {
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
                    GitCommandRunner.execute(repoPath, "git", "init");
                    GitCommandRunner.execute(repoPath, "git", "checkout", "-b", "main");
                    GitCommandRunner.execute(repoPath, "git", "config", "--local", "http.sslVerify", "false");


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
        String remoteUrl = GitCommandRunner.execute(repoPath, "git", "config", "--get", "remote.origin.url").trim();

        if (remoteUrl.isEmpty()) {
            remoteUrl = com.intellij.openapi.ui.Messages.showInputDialog(
                    p,
                    "No remote repository is configured for this project.\n\nPlease enter your Git Remote URL (e.g., https://github.com/user/repo.git):",
                    "Configure Remote",
                    com.intellij.openapi.ui.Messages.getQuestionIcon()
            );

            if (remoteUrl == null || remoteUrl.trim().isEmpty()) {
                Services.getInstance(p, Notifier.class).warn(p, "Push Aborted", "A remote URL is required to push.");
                return;
            }

            final String finalRemoteUrl = remoteUrl.trim();

            ProgressManager.getInstance().run(new Task.Backgroundable(p, "Configuring remote", false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        GitCommandRunner.execute(repoPath, "git", "remote", "add", "origin", finalRemoteUrl);
                        executeGitPush(p, repoPath);
                    } catch (final Exception ex) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).error(p, "Git Error", "Failed to add remote: " + ex.getMessage())
                        );
                    }
                }
            });
        } else {
            executeGitPush(p, repoPath);
        }
    }

    private void executeGitPush(final @NotNull Project p, final Path repoPath) {
        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Pushing to Remote", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                GitCommandRunner.execute(repoPath, "git", "branch", "-M", "main");
                indicator.setText("Syncing with remote (Pull --rebase)...");
                GitCommandRunner.execute(repoPath, "git", "pull", "--rebase", "--autostash", "origin", "main");
                GitCommandRunner.execute(repoPath, "git", "rebase", "--abort");

                indicator.setText("Pushing commits...");
                GitCommandRunner.execute(repoPath, "git", "push", "-u", "origin", "main");

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
                            String scope = setGlobally ? "--global" : "--local";
                            GitCommandRunner.execute(repoPath, "git", "config", scope, "user.name", name.trim());
                            GitCommandRunner.execute(repoPath, "git", "config", scope, "user.email", email.trim());

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

    private static class GitIdentityDialog extends DialogWrapper {
        private final JBTextField nameField = new JBTextField();
        private final JBTextField emailField = new JBTextField();
        private final JBCheckBox globalCheckBox = new JBCheckBox("Set globally");

        public GitIdentityDialog(@Nullable Project p) {
            super(p, true);
            setTitle("Set Git Identity and Commit");
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            return FormBuilder.createFormBuilder()
                    .addLabeledComponent("Name:", nameField)
                    .addLabeledComponent("Email:", emailField)
                    .addComponent(globalCheckBox)
                    .getPanel();
        }

        @Override
        public @Nullable JComponent getPreferredFocusedComponent() {
            return nameField;
        }

        public String getUserName() {
            return nameField.getText();
        }

        public String getUserEmail() {
            return emailField.getText();
        }

        public boolean isSetGlobalConfig() {
            return globalCheckBox.isSelected();
        }
    }
}