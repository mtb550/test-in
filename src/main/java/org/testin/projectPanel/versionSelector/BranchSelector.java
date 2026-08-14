package org.testin.projectPanel.versionSelector;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    private final @NotNull ProjectPanel pp;
    private final @NotNull GitRepositoryService git;
    private final @NotNull ComboBox<String> comboBox;
    private final @NotNull DefaultComboBoxModel<String> model;

    /**
     * Null while no project is selected, or the selected one has no path.
     */
    private @Nullable Path projectPath;

    // Written from background git tasks and read on the EDT. Empty, never null,
    // when no branch is known yet.
    private volatile @NotNull String currentBranch = "";

    private boolean isUpdating = false;

    public BranchSelector(final @NotNull Project p, final @NotNull ProjectPanel pp,
                          final @Nullable TestProjectDirectoryDto testProjectDirectory) {
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

    public void updateProject(final @Nullable TestProjectDirectoryDto testProjectDirectory) {
        final Path path = testProjectDirectory != null ? testProjectDirectory.getPath() : null;
        this.projectPath = path;

        isUpdating = true;
        try {
            model.removeAllElements();
            comboBox.setEnabled(false);
            currentBranch = "";
        } finally {
            isUpdating = false;
        }

        if (path == null) {
            showPlaceholder("No project path found");
        } else if (git.isRepository(path)) {
            showPlaceholder("Loading branches..");
            loadGitBranches(path);
        } else {
            showPlaceholder("Not a Git repository");
        }
    }

    /**
     * Adds a non-branch entry without letting the selection listener treat it as
     * a checkout request.
     */
    private void showPlaceholder(final @NotNull String text) {
        isUpdating = true;
        try {
            model.addElement(text);
        } finally {
            isUpdating = false;
        }
    }

    private void onSelection(final @NotNull ActionEvent e) {
        if (isUpdating) return;

        final String selectedBranch = getSelectedBranch();

        if (selectedBranch == null || selectedBranch.equals("No branches found") ||
                selectedBranch.equals("Loading branches..") || selectedBranch.equals(currentBranch)) {
            return;
        }

        checkoutBranchAndRefreshTree(selectedBranch);
    }

    private void checkoutBranchAndRefreshTree(final @NotNull String targetBranch) {
        // Captured before the task starts: the field can be reassigned by a
        // project switch while the checkout is still running.
        final Path repositoryPath = projectPath;
        if (repositoryPath == null) return;

        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Checking out branch: " + targetBranch, false) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    currentBranch = git.checkout(repositoryPath, targetBranch);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        final TestProjectDirectoryDto currentProject = pp.getTestProjectSelector().getSelectedTestProject().getItem();
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

    private void loadGitBranches(final @NotNull Path repositoryPath) {
        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Fetching Git branches", false) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    try {
                        git.fetchRemoteBranches(repositoryPath);
                    } catch (final Exception fetchError) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).warn(p, "Git Fetch Warning", "Could not refresh remote branches: " + fetchError.getMessage()));
                    }
                    final List<String> branches = git.getAvailableBranches(repositoryPath);
                    final String loadedCurrentBranch = git.getCurrentBranch(repositoryPath);
                    if (loadedCurrentBranch != null) currentBranch = loadedCurrentBranch;

                    ApplicationManager.getApplication().invokeLater(() -> {
                        isUpdating = true;
                        try {
                            model.removeAllElements();

                            if (!branches.isEmpty()) {
                                for (final String branch : branches) {
                                    model.addElement(branch);
                                }

                                if (!currentBranch.isEmpty() && branches.contains(currentBranch)) {
                                    comboBox.setSelectedItem(currentBranch);
                                } else {
                                    comboBox.setSelectedIndex(0);
                                    final String selected = getSelectedBranch();
                                    currentBranch = selected == null ? "" : selected;
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

    public @NotNull JComponent getComponent() {
        return comboBox;
    }

    public @Nullable String getSelectedBranch() {
        return (String) comboBox.getSelectedItem();
    }
}
