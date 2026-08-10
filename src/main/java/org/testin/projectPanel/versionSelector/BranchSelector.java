package org.testin.projectPanel.versionSelector;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;
import org.testin.git.GitRepositoryService;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.projectPanel.ProjectPanel;
import org.testin.services.Services;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.List;

public class BranchSelector {
    private final @NotNull Project p;
    private final ProjectPanel pp;
    private final GitRepositoryService git;
    private final ComboBox<String> comboBox;
    private final DefaultComboBoxModel<String> model;

    private Path projectPath;
    // Written from background git tasks and read on the EDT.
    private volatile String currentBranch = "";

    private boolean isUpdating = false;

    public BranchSelector(final @NotNull Project p, final ProjectPanel pp, final TestProjectDirectoryDto testProjectDirectory) {
        this.p = p;
        this.pp = pp;
        this.git = new GitRepositoryService(p);
        this.model = new DefaultComboBoxModel<>();
        this.comboBox = new ComboBox<>(model);

        comboBox.setFocusable(false);
        comboBox.setEnabled(false);

        comboBox.addActionListener(this::onSelection);

        updateProject(testProjectDirectory);
    }

    public void updateProject(final TestProjectDirectoryDto testProjectDirectory) {
        this.projectPath = testProjectDirectory != null ? testProjectDirectory.getPath() : null;

        isUpdating = true;
        try {
            model.removeAllElements();
            comboBox.setEnabled(false);
            currentBranch = "";
        } finally {
            isUpdating = false;
        }

        if (projectPath != null) {
            if (git.isRepository(projectPath)) {
                isUpdating = true;
                model.addElement("Loading branches...");
                isUpdating = false;

                loadGitBranches();
            } else {
                isUpdating = true;
                model.addElement("Not a Git repository");
                isUpdating = false;
            }
        } else {
            isUpdating = true;
            model.addElement("No project path found");
            isUpdating = false;
        }
    }

    private void onSelection(final ActionEvent e) {
        if (isUpdating) return;

        String selectedBranch = getSelectedBranch();

        if (selectedBranch == null || selectedBranch.equals("No branches found") ||
                selectedBranch.equals("Loading branches...") || selectedBranch.equals(currentBranch)) {
            return;
        }

        checkoutBranchAndRefreshTree(selectedBranch);
    }

    private void checkoutBranchAndRefreshTree(final String targetBranch) {
        if (projectPath == null) return;

        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Checking out branch: " + targetBranch, false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    currentBranch = git.checkout(projectPath, targetBranch);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        TestProjectDirectoryDto currentProject = pp.getTestProjectSelector().getSelectedTestProject().getItem();
                        if (currentProject != null) {
                            pp.getProjectTree().refresh();
                        }
                    });

                } catch (final Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        isUpdating = true;
                        try {
                            if (!currentBranch.isEmpty()) {
                                comboBox.setSelectedItem(currentBranch);
                            }
                        } finally {
                            isUpdating = false;
                        }

                        Services.getInstance(p, Notifier.class).error(p, "Git Checkout Failed", "Could not checkout " + targetBranch + ". Do you have uncommitted changes?\n" + ex.getMessage());
                    });
                }
            }
        });
    }

    private void loadGitBranches() {
        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Fetching Git branches", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    try {
                        git.fetchRemoteBranches(projectPath);
                    } catch (final Exception fetchError) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).warn(p, "Git Fetch Warning", "Could not refresh remote branches: " + fetchError.getMessage()));
                    }
                    List<String> branches = git.getAvailableBranches(projectPath);
                    String loadedCurrentBranch = git.getCurrentBranch(projectPath);
                    if (loadedCurrentBranch != null) currentBranch = loadedCurrentBranch;

                    ApplicationManager.getApplication().invokeLater(() -> {
                        isUpdating = true;
                        try {
                            model.removeAllElements();

                            if (!branches.isEmpty()) {
                                for (String branch : branches) {
                                    model.addElement(branch);
                                }

                                if (!currentBranch.isEmpty() && branches.contains(currentBranch)) {
                                    comboBox.setSelectedItem(currentBranch);
                                } else {
                                    comboBox.setSelectedIndex(0);
                                    currentBranch = (String) comboBox.getSelectedItem();
                                }

                                comboBox.setEnabled(true);
                            } else {
                                model.addElement("No branches found");
                                comboBox.setEnabled(false);
                            }
                        } finally {
                            isUpdating = false;
                        }
                    });

                } catch (final Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        isUpdating = true;
                        try {
                            model.removeAllElements();
                            model.addElement("Failed to load branches");
                            comboBox.setEnabled(false);
                        } finally {
                            isUpdating = false;
                        }
                        Services.getInstance(p, Notifier.class).error(p, "Git Error", "Failed to load branches: " + ex.getMessage());
                    });
                }
            }
        });
    }

    public JComponent getComponent() {
        return comboBox;
    }

    public String getSelectedBranch() {
        return (String) comboBox.getSelectedItem();
    }
}
