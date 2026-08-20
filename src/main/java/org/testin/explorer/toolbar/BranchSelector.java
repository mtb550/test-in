package org.testin.explorer.toolbar;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.explorer.ExplorerPanel;
import org.testin.git.GitRepositoryService;
import org.testin.git.ViewPendingCommitsAction;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.services.Services;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.List;

public class BranchSelector {
    private final @NotNull Project p;
    private final @NotNull ExplorerPanel pp;
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

    /**
     * True while the box holds an explanation rather than branches - loading,
     * no project, not a repository, nothing found.
     * <p>
     * A field rather than a comparison against the text on screen. That is what
     * it used to be, and it guarded only two of the four messages: selecting
     * "Not a Git repository" was read as a request to check out a branch by that
     * name. The strings are also shown to the tester, so rewording one silently
     * broke the guard - which is exactly what nearly happened when the ellipsis
     * in "Loading branches..." was corrected in two places at once.
     */
    private boolean showingPlaceholder = false;

    public BranchSelector(final @NotNull Project p, final @NotNull ExplorerPanel pp,
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
            showPlaceholder("Loading branches...");
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
            showingPlaceholder = true;
        } finally {
            isUpdating = false;
        }
    }

    /**
     * The event is reported as unused, and the parameter stays: this is an
     * {@code ActionListener} target, so the signature is the contract rather
     * than a choice. The selected branch comes from the box, not the event (#61).
     */
    private void onSelection(final @NotNull ActionEvent e) {
        if (isUpdating) return;

        final String selectedBranch = getSelectedBranch();

        if (selectedBranch == null || showingPlaceholder || selectedBranch.equals(currentBranch)) {
            return;
        }

        checkoutBranchAndRefreshTree(selectedBranch);
    }

    /**
     * Checks the branch out and re-reads everything that came with it.
     * <p>
     * Rebuilding the tree is not enough and never was. The tree is drawn from
     * the indexer's cache, and a checkout replaces every file under the project
     * - so a tree redrawn from the old cache shows the test cases of the branch
     * that was left, misses the ones only the new branch has, and reads stale
     * descriptions for the ones on both. Everything downstream of the cache -
     * the editors, the details panel, the reports - was reading the old branch
     * too (#88).
     * <p>
     * So the switch does what Refresh does, through the same action rather than
     * a copy of it: the VFS is told the files changed, the index is thrown away
     * and rebuilt with a progress bar, editors on nodes the new branch does not
     * have are closed, and the tree is rebuilt from what was actually read.
     */
    private void checkoutBranchAndRefreshTree(final @NotNull String targetBranch) {
        // Captured before the task starts: the field can be reassigned by a
        // project switch while the checkout is still running.
        final Path repositoryPath = projectPath;
        if (repositoryPath == null) return;

        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Checking branch " + targetBranch, false) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                final int pending = (int) git.status(repositoryPath).stream().filter(line -> !line.isBlank()).count();

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (pending == 0) {
                        checkout(repositoryPath, targetBranch);
                        return;
                    }
                    askBeforeCarryingWorkAcross(repositoryPath, targetBranch, pending);
                });
            }
        });
    }

    /**
     * Asks before a switch takes uncommitted work with it.
     * <p>
     * Git hardly ever refuses. A new test case is an untracked file and comes
     * along without a word; an edited one comes along too unless the file
     * differs on the branch being entered, which is the one case Git stops. So
     * the common outcome is a tester landing on another branch with work that
     * belongs to the one they left - and here that work looks like it belongs
     * where it landed, because the tree shows it and the review offers it for
     * commit.
     * <p>
     * The box goes back to the current branch first, so a question left
     * unanswered leaves the panel saying where the repository actually is. A
     * switch that goes ahead puts it right again when the tree is rebuilt.
     */
    private void askBeforeCarryingWorkAcross(final @NotNull Path repositoryPath, final @NotNull String targetBranch,
                                             final int pending) {
        restoreSelectedBranch();

        final String changes = pending == 1 ? "1 change" : pending + " changes";

        new ConfirmDialog(p, "Uncommitted Changes",
                changes + " in this test project are not committed. Switching does not leave them behind - "
                        + "they come with you, and can be committed onto " + targetBranch + " by mistake.",
                currentBranch, targetBranch,
                "Switch Anyway", () -> checkout(repositoryPath, targetBranch),
                List.of(new ConfirmDialog.Alternative(Shortcuts.ConfirmAlternative, "Review Changes",
                        () -> new ViewPendingCommitsAction(p, pp.getProjectTree().getMainTree()).openFor(repositoryPath))))
                .show();
    }

    private void checkout(final @NotNull Path repositoryPath, final @NotNull String targetBranch) {
        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Checking out branch: " + targetBranch, false) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                // Null means the checkout did not happen; the git reason is
                // already in testin.log, and the sentence worth showing is the
                // one below rather than the command's output (#63).
                final @Nullable String checkedOut = git.checkout(repositoryPath, targetBranch);
                if (checkedOut == null) {
                    ApplicationManager.getApplication().invokeLater(() -> refuseSwitch(repositoryPath, targetBranch));
                    return;
                }

                currentBranch = checkedOut;

                // The files changed underneath the IDE, which knows nothing about
                // a checkout the plugin ran as a command. Through the indexer,
                // which owns file access, and before the re-index reads them.
                Services.getInstance(p, ProjectIndexer.class).refreshDirectory(repositoryPath);

                ApplicationManager.getApplication().invokeLater(() -> pp.reindex("Switched to " + checkedOut));
            }
        });
    }

    /**
     * What a refused checkout says. Git refuses when the switch would overwrite
     * uncommitted work, which here means edited test cases - so the message
     * names that as the cause and carries the review that clears it, instead of
     * asking the tester a question about their own repository.
     */
    private void refuseSwitch(final @NotNull Path repositoryPath, final @NotNull String targetBranch) {
        restoreSelectedBranch();

        final Notifier notifier = Services.getInstance(p, Notifier.class);
        notifier.warnWithAction(p, "Branch Not Switched",
                targetBranch + " was not checked out. There are uncommitted changes in this test project "
                        + "that switching would overwrite - commit them first.",
                "Review Changes",
                // Built on the panel's own tree: the review belongs to the
                // project the tree is showing, which is the one whose branch
                // would not switch.
                () -> new ViewPendingCommitsAction(p, pp.getProjectTree().getMainTree()).openFor(repositoryPath));
    }

    /**
     * Puts the box back on the branch that is actually checked out — the failed
     * selection is still showing, and leaving it there says the checkout worked.
     */
    private void restoreSelectedBranch() {
        isUpdating = true;
        try {
            if (!currentBranch.isEmpty()) {
                comboBox.setSelectedItem(currentBranch);
            }
        } finally {
            isUpdating = false;
        }
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
                                showingPlaceholder = false;
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
                                showingPlaceholder = true;
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
