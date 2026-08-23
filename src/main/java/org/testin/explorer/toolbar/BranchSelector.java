package org.testin.explorer.toolbar;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;
import org.testin.config.ConnectionType;
import org.testin.config.TestinConfigService;
import org.testin.explorer.ExplorerPanel;
import org.testin.git.GitRepositoryService;
import org.testin.git.ViewPendingCommitsAction;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.services.Services;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Optional;
import java.util.Objects;
import java.nio.file.Path;
import java.util.List;

public class BranchSelector {
    private final @NotNull Project p;
    private final @NotNull ExplorerPanel pp;
    private final @NotNull GitRepositoryService git;
    private final @NotNull ComboBox<String> comboBox;
    private final @NotNull DefaultComboBoxModel<String> model;

    /**
     * The repository this box is showing branches of, and the empty path while
     * there is none - the same "nothing configured" the Testin root uses, so
     * there is one shape of absence rather than two (#71).
     */
    private @NotNull Path projectPath = Path.of("");

    // Written from background git tasks and read on the EDT. Empty, never null,
    // when no branch is known yet.
    private volatile @NotNull String currentBranch = "";

    /**
     * The branches the box is currently showing, so a second pass that brings
     * nothing new can leave it untouched. Empty while it holds a placeholder.
     */
    private volatile @NotNull List<String> shown = List.of();

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
                          final @NotNull Optional<TestProjectDirectoryDto> testProjectDirectory) {
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

    public void updateProject(final @NotNull Optional<TestProjectDirectoryDto> testProjectDirectory) {
        final @NotNull Path path = testProjectDirectory.map(TestProjectDirectoryDto::getPath).orElse(Path.of(""));
        this.projectPath = path;

        currentBranch = "";

        // What testin.yml says this project is decides whether branches are its
        // business at all. A server-hosted project has none, so the box is not
        // shown and nothing here reaches a Git remote - which is the whole point
        // of asking the channel rather than asking the folder.
        final @NotNull ConnectionType connection =
                Services.getInstance(p, TestinConfigService.class).get().connection();

        final boolean showable = connection.isShowsBranches()
                && !path.toString().isEmpty()
                && !git.isNotRepository(path);

        comboBox.setVisible(showable);

        if (!showable) {
            // Still said, for the log and for a project declared as Git that has
            // not been cloned here yet.
            showPlaceholder(connection.isShowsBranches() ? "Not a Git repository" : "Not shared through Git");
            return;
        }

        showPlaceholder("Loading branches...");
        loadGitBranches(path);
    }

    /**
     * Replaces whatever the box holds with an explanation, and marks it as one.
     * <p>
     * The whole job, not half of it: clearing the box, forgetting what was in
     * it, saying it cannot be selected from, and only then putting the words up.
     * It used to add the words while its caller did the clearing, and the two
     * drifted the moment there was something else to forget - a refresh left
     * {@code shown} holding the previous branches, so the load that followed
     * found nothing new to show and left "Loading branches..." on screen with
     * the box disabled.
     * <p>
     * The marking matters just as much: a box holding "Failed to load branches"
     * that did not say it was a placeholder read a click on it as a request to
     * check out a branch by that name.
     */
    private void showPlaceholder(final @NotNull String text) {
        isUpdating = true;
        try {
            model.removeAllElements();
            model.addElement(text);
            shown = List.of();
            showingPlaceholder = true;
            comboBox.setEnabled(false);
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

        final @NotNull String selectedBranch = getSelectedBranch();

        if (selectedBranch.isEmpty() || showingPlaceholder || selectedBranch.equals(currentBranch)) {
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
        final @NotNull Path repositoryPath = projectPath;
        if (repositoryPath.toString().isEmpty()) return;

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

        final @NotNull String changes = pending == 1 ? "1 change" : pending + " changes";

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

                // Empty means the checkout did not happen; the git reason is
                // already in testin.log, and the sentence worth showing is the
                // one below rather than the command's output (#63).
                final @NotNull String checkedOut = git.checkout(repositoryPath, targetBranch);
                if (checkedOut.isEmpty()) {
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

        final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);
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

    /**
     * Fills the box, twice.
     * <p>
     * First from what Git already holds, which needs no network and is on
     * screen immediately. Then from the remote, once the fetch behind it comes
     * back with anything new.
     * <p>
     * It used to fetch first and show nothing until it returned. A fetch can
     * stop to ask for credentials, sit on a host that is not reachable, or take
     * a minute on a large repository - and on this repository it asked for a
     * username while the IDE was frozen, so the box read "Loading branches..."
     * until the IDE was killed (#89). The branches Git already knows are the
     * answer to almost every question anyone asks this box, and they were there
     * the whole time.
     */
    private void loadGitBranches(final @NotNull Path repositoryPath) {
        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Loading Git branches", true) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                indicator.setText("Reading branches");
                readBranchesInto(repositoryPath);

                indicator.setText("Fetching from remote");
                fetchQuietly(repositoryPath);
                if (indicator.isCanceled()) return;

                readBranchesInto(repositoryPath);
            }
        });
    }

    /**
     * Reads the branches Git holds on disk and hands them to the box. No
     * network, so nothing here can hang on a remote.
     */
    private void readBranchesInto(final @NotNull Path repositoryPath) {
        try {
            final @NotNull List<String> branches = git.getAvailableBranches(repositoryPath);
            final @NotNull String loadedCurrentBranch = git.getCurrentBranch(repositoryPath);
            if (!loadedCurrentBranch.isEmpty()) currentBranch = loadedCurrentBranch;

            ApplicationManager.getApplication().invokeLater(() -> showBranches(branches));
        } catch (final Exception ex) {
            Logger.error("Could not read branches: " + ex.getMessage());
            ApplicationManager.getApplication().invokeLater(() -> {
                showPlaceholder("Failed to load branches");
                Services.getInstance(p, Notifier.class)
                        .error(p, "Git Error", "Failed to load branches: " + ex.getMessage());
            });
        }
    }

    /**
     * Brings the remote up to date, and says so rather than failing when it
     * cannot: the box is already showing branches, and a remote that is down is
     * not a reason to take them away.
     */
    private void fetchQuietly(final @NotNull Path repositoryPath) {
        try {
            git.fetchRemoteBranches(repositoryPath);
        } catch (final Exception fetchError) {
            Logger.error("Could not refresh remote branches: " + fetchError.getMessage());
            ApplicationManager.getApplication().invokeLater(() ->
                    Services.getInstance(p, Notifier.class).warn(p, "Git Fetch Warning",
                            "Could not refresh remote branches: " + fetchError.getMessage()));
        }
    }

    /**
     * The one owner of what the box holds. Both passes come through here, and
     * so does every future caller: filling a combo box means suppressing its
     * own listener, deciding what stays selected and whether it is selectable
     * at all, and a second copy of that is how a placeholder became a checkout
     * request the first time.
     * <p>
     * A second pass that brings nothing new leaves the box alone entirely. The
     * tester may have opened it, or picked a branch that is checking out, and
     * rebuilding the model underneath them would take that back.
     */
    private void showBranches(final @NotNull List<String> branches) {
        if (branches.isEmpty()) {
            showPlaceholder("No branches found");
            return;
        }
        if (branches.equals(shown)) return;

        isUpdating = true;
        try {
            model.removeAllElements();
            for (final String branch : branches) {
                model.addElement(branch);
            }

            if (branches.contains(currentBranch)) {
                comboBox.setSelectedItem(currentBranch);
            } else {
                comboBox.setSelectedIndex(0);
                currentBranch = getSelectedBranch();
            }

            shown = List.copyOf(branches);
            showingPlaceholder = false;
            comboBox.setEnabled(true);
        } finally {
            isUpdating = false;
        }
    }


    public @NotNull JComponent getComponent() {
        return comboBox;
    }

    /**
     * What the box is showing, and the empty string when it is showing nothing.
     */
    public @NotNull String getSelectedBranch() {
        return Objects.toString(comboBox.getSelectedItem(), "");
    }
}
