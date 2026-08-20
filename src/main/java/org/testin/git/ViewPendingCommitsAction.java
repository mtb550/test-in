package org.testin.git;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.config.TestinConfigService;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.explorer.ExplorerPanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Review-and-push workflow for changed test cases: scan, review dialog, commit,
 * remote configuration, pull-rebase + push, and conflict handling. Every
 * background step runs through {@link GitBackgroundTask}.
 */
public class ViewPendingCommitsAction extends AbstractProjectTreeAction {
    private final @NotNull GitRepositoryService git;
    private final @NotNull GitCommitService commits;

    /**
     * Live only between a successful commit and the push that expires it.
     */
    public ViewPendingCommitsAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, tree, "View Pending Commits", "Review and push changed test cases", AllIcons.Actions.Commit);
        this.git = new GitRepositoryService(p);
        this.commits = new GitCommitService(p);
    }

    /**
     * The remote URL as the tester types it, or empty when they close the prompt.
     * Empty rather than null, so the caller that decides what to do about a
     * missing URL does it in one place whichever way it came up missing.
     */
    private static @NotNull String askForRemoteUrl(final @NotNull Project p) {
        final String typed = Messages.showInputDialog(
                p,
                "No remote repository is configured for this project.\n\nPlease enter your Git Remote URL (e.g., https://github.com/user/repo.git):",
                "Configure Remote",
                Messages.getQuestionIcon());

        return typed == null ? "" : typed.trim();
    }

    /**
     * How a commit is named to the tester. The id when Git could give one - it is
     * what they search for on the remote - and a plain phrase when it could not,
     * so a successful push is never reported as "Commit  is on origin/main".
     */
    private static @NotNull String commitLabel(final @NotNull String commitId) {
        return commitId.isBlank() ? "The commit" : "Commit " + commitId;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
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
        final Path path = TreeValueUtil.projectPath(tree);
        if (path == null) return;

        openFor(path);
    }

    /**
     * The review for a repository the caller already knows, rather than for
     * whatever the tree has selected.
     * <p>
     * A branch that would not switch has the path and a reason to offer the
     * review, and nothing selected to read one from. The menu entry goes through
     * here too, once it has resolved its selection to a repository - the review
     * is about a repository either way.
     */
    public void openFor(final @NotNull Path path) {
        if (!git.isRepository(path)) {
            Services.getInstance(p, Notifier.class).warnWithAction(p,
                    "Git repository not found",
                    "The selected project (" + path.getFileName() + ") is not a Git repository.",
                    "Initialize Git (git init)",
                    () -> initializeGitRepository(p, path)
            );

            return;
        }

        scanForChanges(p, path);
    }

    private void scanForChanges(final @NotNull Project p, final @NotNull Path path) {
        GitBackgroundTask.run(p, "Scanning for changes", true,
                indicator -> {
                    final List<PendingChange> changes = GitDiffProcessor.getPendingChanges(p, path);
                    ApplicationManager.getApplication().invokeLater(() -> reviewChanges(p, path, changes));
                },
                ex -> Services.getInstance(p, Notifier.class).error(p, "Git Error", "Failed to calculate diffs: " + ex.getMessage()));
    }

    private void reviewChanges(final @NotNull Project p, final @NotNull Path path,
                               final @NotNull List<PendingChange> changes) {
        if (changes.isEmpty()) {
            Services.getInstance(p, Notifier.class).softShow(p, "No changes");
            return;
        }

        // The dialog owns the whole review - which changes, the message, and
        // whether it goes to the remote - so there is nothing left to ask
        // afterward.
        new PendingCommitsDialog(p, changes, path,
                request -> commitOnBranch(p, path, request)).show();
    }

    /**
     * Puts the review's changes on the branch the review named.
     * <p>
     * Three cases and one of them is the ordinary one. The branch that is
     * already checked out commits as it always did. A name that was not on the
     * list starts a branch here and takes the uncommitted work along, which is
     * how a cycle's results stay off main without leaving the dialog. An
     * existing branch is checked out first - and Git can refuse that, when the
     * switch would overwrite the very changes being committed, so the refusal is
     * reported and nothing is committed anywhere.
     * <p>
     * Off the EDT, because all three ask Git.
     */
    private void commitOnBranch(final @NotNull Project p, final @NotNull Path repoPath,
                                final @NotNull PendingCommitsDialog.Request request) {
        final String target = request.branch();

        GitBackgroundTask.run(p, "Preparing the branch", false,
                indicator -> {
                    final String current = git.getCurrentBranch(repoPath);

                    if (target.isEmpty() || target.equals(current)) {
                        performCommitWorkflow(p, repoPath, request, target.isEmpty() ? current : target);
                        return;
                    }

                    indicator.setText((request.newBranch() ? "Starting " : "Checking out ") + target);

                    final boolean moved = request.newBranch()
                            ? git.startBranch(repoPath, target)
                            : git.checkout(repoPath, target) != null;

                    if (!moved) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).error(p, "Branch Not Switched",
                                        target + " could not be checked out, so nothing was committed. "
                                                + "The changes are still here and still yours."));
                        return;
                    }

                    // A branch started here begins at the commit that is already
                    // checked out, so not one file changed and there is nothing
                    // to read again - the panel is only redrawn so its branch box
                    // stops naming the branch that was left. Moving to a branch
                    // that already existed is the other thing entirely: every
                    // file under the project was just replaced.
                    if (!request.newBranch()) {
                        Services.getInstance(p, ProjectIndexer.class).refreshDirectory(repoPath);
                    }

                    ApplicationManager.getApplication().invokeLater(() -> {
                        final ExplorerPanel panel = Services.getInstance(p, ExplorerPanel.class);

                        if (request.newBranch()) panel.refresh();
                        else panel.reindex("Switched to " + target);

                        performCommitWorkflow(p, repoPath, request, target);
                    });
                },
                ex -> Services.getInstance(p, Notifier.class).error(p, "Git Error",
                        "Could not prepare " + target + ": " + ex.getMessage()));
    }

    private void performCommitWorkflow(
            final @NotNull Project p,
            final @NotNull Path repoPath,
            final @NotNull PendingCommitsDialog.Request request,
            final @Nullable String branch) {
        final String commitMessage = request.message();
        final Collection<PendingChange> selectedChanges = request.changes();
        final boolean push = request.push();

        GitBackgroundTask.run(p, push ? "Committing and pushing" : "Committing to local Git", false,
                indicator -> {
                    indicator.setText("Staging and committing files");
                    commits.stageAndCommit(repoPath, commitMessage, selectedChanges);

                    // Read here, while the commit just made is still HEAD: the
                    // tester is told which commit their changes went into, and a
                    // push that follows reports the same one.
                    final String commitId = commits.headCommitId(repoPath);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (push) {
                            pushToRemote(p, repoPath, commitId, branch);
                            return;
                        }

                        Services.getInstance(p, Notifier.class).softShow(p, "Committed", commitLabel(commitId));
                    });
                },
                ex -> {
                    if (isIdentityError(ex.getMessage())) {
                        promptAndSetGitIdentity(p, repoPath, request, branch);
                    } else {
                        Services.getInstance(p, Notifier.class).error(p, "Commit Failed", "Failed to commit changes:" + System.lineSeparator() + ex.getMessage());
                    }
                });
    }

    private void initializeGitRepository(final @NotNull Project p, final @NotNull Path repoPath) {
        GitBackgroundTask.run(p, "Initializing git repository", false,
                indicator -> {
                    commits.initialize(repoPath);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Services.getInstance(p, Notifier.class).softShow(p, "Git initialized");

                        // The tester asked to see pending commits. Initializing was
                        // what stood in the way, not what they wanted, so the review
                        // they invoked opens rather than making them ask twice.
                        scanForChanges(p, repoPath);
                    });
                },
                ex -> Services.getInstance(p, Notifier.class).error(p, "Git Init Failed", "Failed to initialize repository: " + ex.getMessage()));
    }

    /**
     * @param committedOn the branch the commit went onto, or null when Git could
     *                    not say which one that was. A push follows the commit
     *                    rather than the remote's default: they are the same
     *                    branch on almost every push, and on the one that
     *                    matters - a cycle committed onto its own branch - the
     *                    default would send the work somewhere the tester did
     *                    not choose
     */
    private void pushToRemote(final @NotNull Project p, final @NotNull Path repoPath,
                              final @NotNull String commitId, final @Nullable String committedOn) {
        GitBackgroundTask.run(p, "Checking Git remote", false,
                indicator -> {
                    final String remoteName = git.getRemoteName(repoPath);
                    final String remoteUrl = remoteName == null ? "" : git.getRemoteUrl(repoPath, remoteName);
                    final String branch = committedOn == null || committedOn.isBlank()
                            ? git.getDefaultBranch(repoPath)
                            : committedOn;
                    if (branch == null || branch.isBlank()) {
                        throw new IllegalStateException("Could not determine which branch to push.");
                    }
                    ApplicationManager.getApplication().invokeLater(() -> {
                        // A null remote name always yields an empty URL above; naming it here
                        // keeps the "already configured" branch provably non-null.
                        if (remoteName == null || remoteUrl.isEmpty()) {
                            configureRemoteAndPush(p, repoPath, remoteName == null ? "origin" : remoteName, branch, commitId);
                        } else {
                            executeGitPush(p, repoPath, remoteName, branch, commitId);
                        }
                    });
                },
                ex -> Services.getInstance(p, Notifier.class).error(p, "Git Error", "Could not read the Git remote: " + ex.getMessage()));
    }

    private void configureRemoteAndPush(final @NotNull Project p, final @NotNull Path repoPath,
                                        final @NotNull String remoteName, final @NotNull String branch,
                                        final @NotNull String commitId) {
        final TestinConfigService config = Services.getInstance(p, TestinConfigService.class);

        // The repository already says where its test project lives, so a clone of
        // it should not have to be told again. Asking is the fallback, not the
        // first move (#8).
        final String known = config.get().testinRepoUrl();
        final String remoteUrl = known.isEmpty() ? askForRemoteUrl(p) : known;

        if (remoteUrl.isEmpty()) {
            Services.getInstance(p, Notifier.class).warn(p, "Push Aborted", "A remote URL is required to push.");
            return;
        }

        // Written back so the next machine that opens this repository inherits it.
        // Only what the tester typed: a URL that came out of the file is already
        // in it.
        if (known.isEmpty()) config.rememberRepoUrl(remoteUrl);

        GitBackgroundTask.run(p, "Configuring remote", false,
                indicator -> {
                    commits.configureRemote(repoPath, remoteName, remoteUrl);
                    ApplicationManager.getApplication().invokeLater(() -> executeGitPush(p, repoPath, remoteName, branch, commitId));
                },
                ex -> Services.getInstance(p, Notifier.class).error(p, "Git Error", "Failed to add remote: " + ex.getMessage()));
    }

    private void executeGitPush(final @NotNull Project p, final @NotNull Path repoPath,
                                final @NotNull String remote, final @NotNull String branch,
                                final @NotNull String commitId) {
        GitBackgroundTask.run(p, "Pushing to Remote", false,
                indicator -> {
                    indicator.setText("Syncing with remote: pull --rebase, then push");
                    commits.pullAndPush(repoPath, remote, branch);
                    // In the log for the same reason the sync is: the push
                    // finishes on its own time, not under the tester's hand -
                    // and it names the commit, so the tester can find it on the
                    // remote without going back to look it up.
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(p, Notifier.class).info(p, "Pushed",
                                    commitLabel(commitId) + " is on " + remote + "/" + branch));
                },
                ex -> {
                    if (git.hasConflicts(repoPath)) {
                        showConflictActions(repoPath, remote, branch);
                        return;
                    }

                    // The commit already happened, so there is nothing pending to
                    // review and no second route back to a push. The retry travels
                    // with the failure that needs it.
                    final Notifier notifier = Services.getInstance(p, Notifier.class);
                    notifier.errorWithActions(p, "Push Failed", ex.getMessage(),
                            notifier.action("Try Again", () -> pushToRemote(p, repoPath, commitId, branch)));
                });
    }

    private void showConflictActions(final @NotNull Path repoPath, final @NotNull String remote,
                                     final @NotNull String branch) {
        final Notifier notifier = Services.getInstance(p, Notifier.class);
        notifier.warnWithActions(
                p,
                "Git Conflicts",
                "Pull stopped because conflicts must be resolved in the IDE before continuing.",
                notifier.action("Continue rebase", () -> finishRebase(repoPath, remote, branch, false)),
                notifier.action("Abort rebase", () -> finishRebase(repoPath, remote, branch, true)));
    }

    private void finishRebase(final @NotNull Path repoPath, final @NotNull String remote,
                              final @NotNull String branch, final boolean abort) {
        GitBackgroundTask.run(p, abort ? "Aborting rebase" : "Continuing rebase", false,
                indicator -> {
                    // GitTaskWork declares throws so a lambda can report failure
                    // to the task's error handler - which is where the conflict
                    // recovery below lives. The git reason is already logged (#63).
                    if (abort) {
                        if (git.couldNotAbortRebase(repoPath))
                            throw new IllegalStateException("Could not abort the rebase.");
                    } else {
                        if (git.couldNotContinueRebase(repoPath))
                            throw new IllegalStateException("Could not continue the rebase.");
                        commits.push(repoPath, remote, branch);
                    }
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(p, Notifier.class).info(p,
                                    abort ? "Rebase aborted" : "Rebase continued",
                                    abort ? "Nothing was pushed" : "Changes pushed to the remote"));
                },
                ex -> {
                    if (git.hasConflicts(repoPath)) showConflictActions(repoPath, remote, branch);
                    else
                        Services.getInstance(p, Notifier.class).error(p, "Git Conflict Operation Failed", ex.getMessage());
                });
    }

    private void promptAndSetGitIdentity(
            final @NotNull Project p,
            final @NotNull Path repoPath,
            final @NotNull PendingCommitsDialog.Request request,
            final @Nullable String branch) {
        // The dialog validates what it collected - a blank name or email never
        // leaves it - so this is the workflow resuming, not a second check.
        ApplicationManager.getApplication().invokeLater(() -> new GitIdentityDialog(p, identity ->
                GitBackgroundTask.run(p, "Configuring git identity", false,
                        indicator -> {
                            commits.configureIdentity(repoPath, identity.name(), identity.email(), identity.global());
                            ApplicationManager.getApplication().invokeLater(() -> {
                                // The tester is watching: they just filled the dialog
                                // in and the commit resumes on the next line.
                                Services.getInstance(p, Notifier.class).softShow(p, "Identity set");
                                // The branch is settled by now - this is the
                                // same commit resuming, not a second decision.
                                performCommitWorkflow(p, repoPath, request, branch);
                            });
                        },
                        ex -> Services.getInstance(p, Notifier.class).error(p, "Config Failed",
                                "Failed to set Git identity:" + System.lineSeparator() + ex.getMessage()))
        ).show());
    }

    private boolean isIdentityError(final @Nullable String message) {
        if (message == null) return false;

        final String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("author identity unknown")
                || normalized.contains("please tell me who you are");
    }
}
