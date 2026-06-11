package org.testin.projectPanel.versionSelector;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.GitCommandRunner;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BranchSelector {
    private final boolean showRemote = false;

    private final Project project;
    private final ProjectPanel projectPanel;
    private final ComboBox<String> comboBox;
    private final DefaultComboBoxModel<String> model;

    private Path projectPath;
    private String currentBranch = "";

    private boolean isUpdating = false;

    public BranchSelector(final @NotNull Project project, final ProjectPanel projectPanel, final TestProjectDirectoryDto testProjectDirectory) {
        this.project = project;
        this.projectPanel = projectPanel;
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
            File gitDir = new File(projectPath.toFile(), ".git");
            if (gitDir.exists() && gitDir.isDirectory()) {
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

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Checking out branch: " + targetBranch, false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    GitCommandRunner.execute(projectPath, "git", "checkout", targetBranch);
                    currentBranch = targetBranch;

                    ApplicationManager.getApplication().invokeLater(() -> {
                        TestProjectDirectoryDto currentProject = projectPanel.getTestProjectSelector().getSelectedTestProject().getItem();
                        if (currentProject != null) {
                            projectPanel.getTestCaseTreeBuilder().buildTree(currentProject);
                            projectPanel.getTestRunTreeBuilder().buildTree(currentProject);
                        }
                    });

                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        isUpdating = true;
                        try {
                            if (!currentBranch.isEmpty()) {
                                comboBox.setSelectedItem(currentBranch);
                            }
                        } finally {
                            isUpdating = false;
                        }

                        Services.getInstance(project, Notifier.class).error(project, "Git Checkout Failed", "Could not checkout " + targetBranch + ". Do you have uncommitted changes?\n" + ex.getMessage());
                    });
                }
            }
        });
    }

    private void loadGitBranches() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Fetching Git branches", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    String[] command = showRemote
                            ? new String[]{"git", "branch", "-a", "--no-color"}
                            : new String[]{"git", "branch", "--no-color"};

                    String output = GitCommandRunner.execute(projectPath, command);
                    List<String> branches = parseBranches(output);

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

                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        isUpdating = true;
                        try {
                            model.removeAllElements();
                            model.addElement("Failed to load branches");
                            comboBox.setEnabled(false);
                        } finally {
                            isUpdating = false;
                        }
                        Services.getInstance(project, Notifier.class).error(project, "Git Error", "Failed to load branches: " + ex.getMessage());
                    });
                }
            }
        });
    }

    private List<String> parseBranches(final String commandOutput) {
        List<String> branchList = new ArrayList<>();
        if (commandOutput == null || commandOutput.trim().isEmpty()) {
            return branchList;
        }

        String[] lines = commandOutput.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.startsWith("*")) {
                trimmed = trimmed.substring(1).trim();
                currentBranch = trimmed;
            }

            if (showRemote && trimmed.contains("->")) {
                continue;
            }

            if (!branchList.contains(trimmed)) {
                branchList.add(trimmed);
            }
        }
        return branchList;
    }

    public JComponent getComponent() {
        return comboBox;
    }

    public String getSelectedBranch() {
        return (String) comboBox.getSelectedItem();
    }
}