package org.testin.util.git;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.Config;
import org.testin.util.notifications.Notifier;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;

public class ReviewBranchesDialog extends DialogWrapper {

    private final Path repoPath;
    private final List<String> remoteBranches;
    private JBList<String> branchList;

    public ReviewBranchesDialog(@Nullable Project project, Path repoPath, List<String> remoteBranches) {
        super(project, true);
        this.repoPath = repoPath;
        this.remoteBranches = remoteBranches;
        setTitle("Test Lead Dashboard - Pending Reviews");
        setOKButtonText("Review Changes"); // Changed to Review
        setCancelButtonText("Close");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JLabel infoLabel = new JLabel("Select a tester's branch to review test cases line-by-line:");
        panel.add(infoLabel, BorderLayout.NORTH);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String branch : remoteBranches) {
            if (!branch.contains("HEAD") && !branch.endsWith("/master") && !branch.endsWith("/main")) {
                listModel.addElement(branch.trim());
            }
        }

        branchList = new JBList<>(listModel);
        branchList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JBScrollPane(branchList), BorderLayout.CENTER);

        JButton rejectButton = new JButton("Delete Branch Entirely");
        rejectButton.setForeground(Color.RED);
        rejectButton.addActionListener(e -> rejectSelectedBranch());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(rejectButton);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        panel.setPreferredSize(new Dimension(500, 300));
        return panel;
    }

    @Override
    protected void doOKAction() {
        String selectedBranch = branchList.getSelectedValue();
        if (selectedBranch == null) {
            Messages.showWarningDialog("Please select a branch to review.", "No Selection");
            return;
        }

        // Close this dialog and start the review workflow
        super.doOKAction();
        prepareReviewEnvironment(selectedBranch);
    }

    private void rejectSelectedBranch() {
        String selectedBranch = branchList.getSelectedValue();
        if (selectedBranch == null) return;

        int confirm = Messages.showYesNoDialog(
                "Are you sure you want to permanently delete '" + selectedBranch + "' without reviewing?",
                "Confirm Rejection",
                Messages.getWarningIcon()
        );

        if (confirm == Messages.YES) {
            close(CANCEL_EXIT_CODE);
            String branchName = selectedBranch.replace("origin/", "").trim();
            executeReject(branchName);
        }
    }

    private void prepareReviewEnvironment(String remoteBranchName) {
        String branchName = remoteBranchName.replace("origin/", "").trim();

        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Preparing Review", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    indicator.setText("Syncing master...");
                    GitCommandRunner.execute(repoPath, "git", "checkout", "master");
                    GitCommandRunner.execute(repoPath, "git", "pull", "--rebase", "origin", "master");

                    indicator.setText("Loading branch changes...");
                    // Squash brings changes into working directory without committing
                    GitCommandRunner.execute(repoPath, "git", "merge", "--squash", remoteBranchName);

                    // Unstage files so GitDiffProcessor treats them as standard edits
                    try { GitCommandRunner.execute(repoPath, "git", "reset", "HEAD"); } catch (Exception ignored) {}

                    // Fetch the diffs exactly like ViewCommits does!
                    List<TestCaseDiff> changes = GitDiffProcessor.getPendingChanges(repoPath);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (changes.isEmpty()) {
                            Notifier.getInstance().warn("No Changes", "This branch has no differences from master.");
                            abortReview(); // Clean up
                            return;
                        }

                        // Open the per-test-case review UI
                        PendingCommitsDialog dialog = new PendingCommitsDialog(
                                Config.getProject(),
                                changes,
                                repoPath,
                                "Reviewing Branch: " + branchName,
                                "Approve & Merge"
                        );

                        if (dialog.showAndGet()) {
                            // Test Lead clicked Approve! Complete the merge.
                            finalizeMerge(branchName);
                        } else {
                            // Test Lead clicked Cancel. Undo the squash merge.
                            abortReview();
                        }
                    });

                } catch (Exception ex) {
                    abortReview();
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().error("Review Failed", "Could not prepare branch for review:\n" + ex.getMessage())
                    );
                }
            }
        });
    }

    private void finalizeMerge(String branchName) {
        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Merging and Cleaning up", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    indicator.setText("Committing approved changes...");
                    GitCommandRunner.execute(repoPath, "git", "add", ".");
                    GitCommandRunner.execute(repoPath, "git", "commit", "-m", "Merge approved test cases from " + branchName);

                    indicator.setText("Pushing to master...");
                    GitCommandRunner.execute(repoPath, "git", "push", "origin", "master");

                    indicator.setText("Deleting remote branch...");
                    GitCommandRunner.execute(repoPath, "git", "push", "origin", "--delete", branchName);

                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().info("Merge Complete", "Test cases approved and merged into master!")
                    );

                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().error("Merge Failed", "Failed to commit approved changes: " + ex.getMessage())
                    );
                }
            }
        });
    }

    private void abortReview() {
        // Discard the paused merge and return the workspace to a clean master branch
        try {
            GitCommandRunner.execute(repoPath, "git", "reset", "--hard", "HEAD");
        } catch (Exception ignored) {}
    }

    private void executeReject(String branchName) {
        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Deleting branch", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    GitCommandRunner.execute(repoPath, "git", "push", "origin", "--delete", branchName);
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().info("Branch Rejected", "The branch '" + branchName + "' was deleted.")
                    );
                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().error("Delete Failed", "Could not delete branch:\n" + ex.getMessage())
                    );
                }
            }
        });
    }
}