package org.testin.git;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.config.TestinConfigService;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.notifications.Notifier;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Locale;

/**
 * Review-and-push workflow for changed test cases: scan, review dialog, commit,
 * remote configuration, pull-rebase + push, and conflict handling. Every
 * background step runs through {@link GitBackgroundTask}.
 * <p>
 * Declared in {@code plugin.xml} (#119), so Find Action offers it and a tester
 * can bind a key to it - it has never had one. In the main descriptor rather
 * than the Git one, for the reason the menu entry is added in every IDE: an
 * action that vanishes where Git is missing teaches nobody the feature exists,
 * so it is present and grayed with the reason on it (#273).
 * <p>
 * No constructor and no fields: the platform builds one instance for the whole
 * IDE, so the repository comes from the keystroke and the two Git services -
 * each of which is built around one project - belong to {@link Work}.
 */
public class ViewPendingCommitsAction extends DumbAwareAction {

    // UC-SHARE-010
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        TestinData.tree(e).flatMap(TreeValueUtil::projectPath).ifPresent(path -> reviewFor(p, path));
    }

    // UC-SHARE-010
    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Rule-SHARE-105. Grayed with the reason in it when Git is missing,
        // rather than left out of the menu entirely (#273).
        if (!OptionalPlugin.GIT.enableOrExplain(this, e.getPresentation())) return;

        e.getPresentation().setEnabled(TestinData.firstSelected(e, TestProjectDirectoryDto.class).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    /**
     * UC-SHARE-009, Rule-SHARE-042.
     * <p>
     * The review for a repository the caller already knows, rather than for
     * whatever the tree has selected.
     * <p>
     * A branch that would not switch has the path and a reason to offer the
     * review, and nothing selected to read one from. The menu entry comes through
     * here too, once it has resolved its selection to a repository - the review
     * is about a repository either way.
     * <p>
     * A method rather than an action to construct: the branch selector used to
     * build one of these with the main tree in its hands, purely to reach this,
     * and a declared action cannot be constructed at all (#119).
     */
    public static void reviewFor(final @NotNull Project p, final @NotNull Path path) {
        new Work(p).openFor(path);
    }

    /**
     * How a commit is named to the tester. The id when Git could give one - it is
     * what they search for on the remote - and a plain phrase when it could not,
     * so a successful push is never reported as "Commit  is on origin/main".
     */
    private static @NotNull String commitLabel(final @NotNull String commitId) {
        return commitId.isBlank() ? "The commit" : "Commit " + commitId;
    }

    /**
     * Reviewing and pushing one repository, for a project that is there.
     *
     * @param git     what a repository can be asked
     * @param commits the commit and the push, through the one service they go
     *                through everywhere
     */
    private record Work(@NotNull Project p, @NotNull GitRepositoryService git, @NotNull GitCommitService commits) {

        private Work(final @NotNull Project p) {
            this(p, new GitRepositoryService(p), new GitCommitService(p));
        }

        /**
         * UC-SHARE-009, Rule-SHARE-042.
         * <p>
         * The review itself, and the offer to make a repository where there is
         * none yet.
         */
        private void openFor(final @NotNull Path path) {
            if (git.isNotRepository(path)) {
                Services.getInstance(p, Notifier.class).warnWithAction(p,
                        "Git repository not found",
                        "The selected project (" + path.getFileName() + ") is not a Git repository.",
                        "Initialize Git (git init)",
                        () -> initializeGitRepository(path)
                );

                return;
            }

            scanForChanges(path);
        }

        // UC-SHARE-010, Rule-SHARE-050
        private void scanForChanges(final @NotNull Path path) {
            GitBackgroundTask.run(p, "Scanning for changes", true,
                    indicator -> {
                        // Nothing can be committed while a rebase is unfinished, and
                        // Git says so in its own words - "interactive rebase in
                        // progress ... nothing to commit" - after the tester has
                        // picked their changes and typed a message. So the review is
                        // not offered at all; what is offered is the way out of the
                        // rebase, which is the only thing that can happen next (#89).
                        if (git.hasConflicts(path)) {
                            showConflictActions(path, git.getRemoteName(path), git.syncBranch(path));
                            return;
                        }

                        final @NotNull List<PendingChange> changes = GitDiffProcessor.getPendingChanges(p, path);

                        // Read here and carried in, because the dialog cannot ask:
                        // every Git command goes through git4idea's authentication
                        // setup, which asserts it is not running on the EDT, and a
                        // dialog is built on the EDT.
                        final @NotNull List<String> branches = git.getLocalBranches(path);
                        final @NotNull String current = git.getCurrentBranch(path);

                        // A commit that succeeded and a push that failed leave
                        // nothing pending and work that never left the machine.
                        final int unpushed = git.unpushedCount(path);

                        ApplicationManager.getApplication().invokeLater(() ->
                                reviewChanges(path, changes, branches, current, unpushed));
                    },
                    ex -> Services.getInstance(p, Notifier.class).error(p, "Git Error", "Failed to calculate diffs: " + ex.getMessage()));
        }

        // UC-SHARE-010
        private void reviewChanges(final @NotNull Path path, final @NotNull List<PendingChange> changes, final @NotNull List<String> branches, final @NotNull String currentBranch, final int unpushed) {
            if (changes.isEmpty()) {
                offerThePush(path, currentBranch, unpushed);
                return;
            }

            // The dialog owns the whole review - which changes, the message, and
            // whether it goes to the remote - so there is nothing left to ask
            // afterward.
            new PendingCommitsDialog(p, changes, path, branches, currentBranch,
                    request -> commitOnBranch(path, request)).show();
        }

        /**
         * UC-SHARE-014, Rule-SHARE-065.
         * <p>
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
        private void commitOnBranch(final @NotNull Path repoPath, final @NotNull PendingCommitsDialog.Request request) {
            final @NotNull String target = request.branch();

            GitBackgroundTask.run(p, "Preparing the branch", false,
                    indicator -> {
                        final @NotNull String current = git.getCurrentBranch(repoPath);

                        if (target.isEmpty() || target.equals(current)) {
                            performCommitWorkflow(repoPath, request, target.isEmpty() ? current : target);
                            return;
                        }

                        indicator.setText((request.newBranch() ? "Starting " : "Checking out ") + target);

                        final boolean moved = request.newBranch()
                                ? git.startBranch(repoPath, target)
                                : !git.checkout(repoPath, target).isEmpty();

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
                            final @NotNull TreePanel panel = Services.getInstance(p, TreePanel.class);

                            if (request.newBranch()) panel.refresh();
                            else panel.reindex("Switched to " + target);

                            performCommitWorkflow(repoPath, request, target);
                        });
                    },
                    ex -> Services.getInstance(p, Notifier.class).error(p, "Git Error",
                            "Could not prepare " + target + ": " + ex.getMessage()));
        }

        /**
         * UC-SHARE-015, Rule-SHARE-067.
         * <p>
         * What to say when there is nothing to commit.
         * <p>
         * Usually nothing happened and "No changes" is the whole truth. But a commit
         * that succeeded and a push that failed - a conflict, a rejected pull, a
         * dropped connection - leaves exactly this state with work that has not left
         * the machine, and the review saying "No changes" was the last thing the
         * plugin had to offer: the commit existed, nothing was pending, and no
         * action anywhere pushed it (#66).
         */
        private void offerThePush(final @NotNull Path path, final @NotNull String currentBranch, final int unpushed) {
            final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);

            if (unpushed == 0) {
                notifier.softRefuse(p, "No changes");
                return;
            }

            final @NotNull String waiting = unpushed == 1 ? "1 commit is" : unpushed + " commits are";

            notifier.warnWithAction(p, "Not Pushed",
                    waiting + " committed here and not on the remote.",
                    "Push",
                    () -> pushToRemote(path, commits.headCommitId(path), currentBranch));
        }

        // UC-SHARE-013, Rule-SHARE-059
        private void performCommitWorkflow(final @NotNull Path repoPath, final @NotNull PendingCommitsDialog.Request request, final @NotNull String branch) {
            final @NotNull String commitMessage = request.message();
            final @NotNull Collection<PendingChange> selectedChanges = request.changes();
            final boolean push = request.push();

            GitBackgroundTask.run(p, push ? "Committing and pushing" : "Committing to local Git", false,
                    indicator -> {
                        indicator.setText("Staging and committing files");
                        commits.stageAndCommit(repoPath, commitMessage, selectedChanges);

                        // Read here, while the commit just made is still HEAD: the
                        // tester is told which commit their changes went into, and a
                        // push that follows reports the same one.
                        final @NotNull String commitId = commits.headCommitId(repoPath);

                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (push) {
                                pushToRemote(repoPath, commitId, branch);
                                return;
                            }

                            Services.getInstance(p, Notifier.class).softShow(p, "Committed", commitLabel(commitId));
                        });
                    },
                    ex -> {
                        if (isIdentityError(Objects.toString(ex.getMessage(), ""))) {
                            promptAndSetGitIdentity(repoPath, request, branch);
                        } else {
                            Services.getInstance(p, Notifier.class).error(p, "Commit Failed", "Failed to commit changes:" + System.lineSeparator() + ex.getMessage());
                        }
                    });
        }

        // UC-SHARE-009, Rule-SHARE-043
        private void initializeGitRepository(final @NotNull Path repoPath) {
            GitBackgroundTask.run(p, "Initializing git repository", false,
                    indicator -> {
                        commits.initialize(repoPath);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            Services.getInstance(p, Notifier.class).softShow(p, "Git initialized");

                            // The tester asked to see pending commits. Initializing was
                            // what stood in the way, not what they wanted, so the review
                            // they invoked opens rather than making them ask twice.
                            scanForChanges(repoPath);
                        });
                    },
                    ex -> Services.getInstance(p, Notifier.class).error(p, "Git Init Failed", "Failed to initialize repository: " + ex.getMessage()));
        }

        /**
         * UC-SHARE-013.
         *
         * @param committedOn the branch the commit went onto, or null when Git could
         *                    not say which one that was. A push follows the commit
         *                    rather than the remote's default: they are the same
         *                    branch on almost every push, and on the one that
         *                    matters - a cycle committed onto its own branch - the
         *                    default would send the work somewhere the tester did
         *                    not choose
         */
        private void pushToRemote(final @NotNull Path repoPath, final @NotNull String commitId, final @NotNull String committedOn) {
            GitBackgroundTask.run(p, "Checking Git remote", false,
                    indicator -> {
                        final @NotNull String remoteName = git.getRemoteName(repoPath);
                        final @NotNull String remoteUrl = remoteName.isEmpty() ? "" : git.getRemoteUrl(repoPath, remoteName);
                        final @NotNull String branch = committedOn.isBlank() ? git.syncBranch(repoPath) : committedOn;
                        if (branch.isBlank()) {
                            throw new IllegalStateException("Could not determine which branch to push.");
                        }
                        ApplicationManager.getApplication().invokeLater(() -> {
                            // A repository with no remote yields an empty URL above, and
                            // origin is the name the configure step would create.
                            if (remoteUrl.isEmpty()) {
                                configureRemoteAndPush(repoPath, remoteName.isEmpty() ? "origin" : remoteName, branch, commitId);
                            } else {
                                executeGitPush(repoPath, remoteName, branch, commitId);
                            }
                        });
                    },
                    ex -> Services.getInstance(p, Notifier.class).error(p, "Git Error", "Could not read the Git remote: " + ex.getMessage()));
        }

        // UC-SHARE-013, Rule-SHARE-060
        private void configureRemoteAndPush(final @NotNull Path repoPath, final @NotNull String remoteName, final @NotNull String branch, final @NotNull String commitId) {
            final @NotNull TestinConfigService config = Services.getInstance(p, TestinConfigService.class);

            // The repository already says where its test project lives, so a clone of
            // it should not have to be told again. Asking is the fallback, not the
            // first move (#8).
            final @NotNull String known = config.get().repoUrl();

            if (!known.isEmpty()) {
                addRemoteAndPush(repoPath, remoteName, branch, commitId, known);
                return;
            }

            // The dialog refuses an empty field and an address nothing can be pushed
            // to, so what arrives here is a URL. Closing it says nothing: the tester
            // shut the dialog on the question, and the push not happening is the
            // answer to it - which is also why the old "Push Aborted" balloon is
            // gone, since cancelling was the only way to reach it.
            new RemoteUrlDialog(p, remoteName, typed -> {
                // Written back so the next machine that opens this repository
                // inherits it. Only what the tester typed: a URL that came out of
                // the file is already in it.
                config.rememberRepoUrl(typed);
                addRemoteAndPush(repoPath, remoteName, branch, commitId, typed);
            }).show();
        }

        // UC-SHARE-013, Rule-SHARE-060
        private void addRemoteAndPush(final @NotNull Path repoPath, final @NotNull String remoteName, final @NotNull String branch, final @NotNull String commitId, final @NotNull String remoteUrl) {
            GitBackgroundTask.run(p, "Configuring remote", false,
                    indicator -> {
                        commits.configureRemote(repoPath, remoteName, remoteUrl);
                        ApplicationManager.getApplication().invokeLater(() -> executeGitPush(repoPath, remoteName, branch, commitId));
                    },
                    ex -> Services.getInstance(p, Notifier.class).error(p, "Git Error", "Failed to add remote: " + ex.getMessage()));
        }

        // UC-SHARE-013, Rule-SHARE-061
        private void executeGitPush(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch, final @NotNull String commitId) {
            GitBackgroundTask.run(p, "Pushing to Remote", false,
                    indicator -> {
                        indicator.setText("Syncing with remote: pull --rebase, then push");
                        commits.pullAndPush(repoPath, remote, branch);

                        // The pull above rebases a colleague's test cases into the
                        // working tree. Without this the tester read pre-pull data
                        // under a balloon saying the push had succeeded.
                        RepositoryRefresh.after(p, repoPath);
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
                        final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);
                        notifier.errorWithActions(p, "Push Failed", ex.getMessage(),
                                notifier.action("Try Again", () -> pushToRemote(repoPath, commitId, branch)));
                    });
        }

        /**
         * On the background thread that failed, because naming the conflicting files
         * means asking Git for them.
         */
        private void showConflictActions(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch) {
            GitConflictOffer.show(p, git.conflictingPaths(repoPath),
                    () -> resolveConflicts(repoPath, remote, branch),
                    () -> finishRebase(repoPath, remote, branch, false),
                    () -> finishRebase(repoPath, remote, branch, true));
        }

        /**
         * UC-SHARE-017.
         * <p>
         * Merges the conflicted test cases and continues the rebase when nothing is
         * left conflicting.
         * <p>
         * Off the EDT because it reads Git and writes files; the questions it cannot
         * answer open on the EDT from inside.
         */
        private void resolveConflicts(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch) {
            ApplicationManager.getApplication().executeOnPooledThread(() ->
                    ConflictResolution.resolveRebase(p, repoPath,
                            () -> pushAfterRebase(repoPath, remote, branch),
                            leftOver -> Services.getInstance(p, Notifier.class).warn(p, "Still Conflicting",
                                    GitRefs.conflictMessage(leftOver))));
        }

        /**
         * UC-SHARE-017.
         * <p>
         * Pushes once the rebase is through.
         * <p>
         * Separate from {@link #finishRebase} because the rebase is already over by
         * the time this runs - {@link ConflictResolution#resolveRebase} carried it
         * to the end - and asking Git to continue a rebase that has finished fails
         * with "no rebase in progress".
         */
        private void pushAfterRebase(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch) {
            GitBackgroundTask.run(p, "Pushing " + branch, false,
                    indicator -> {
                        commits.push(repoPath, remote, branch);
                        RepositoryRefresh.after(p, repoPath);

                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).info(p, "Rebase continued",
                                        "Changes pushed to the remote"));
                    },
                    ex -> Services.getInstance(p, Notifier.class).error(p, "Push Failed", ex.getMessage()));
        }

        // UC-SHARE-017, Rule-SHARE-077
        private void finishRebase(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch, final boolean abort) {
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

                        // Both endings refresh. The rebase has just written a
                        // colleague's test cases into the working tree, or rolled
                        // the tree back, and neither showed until the IDE happened
                        // to refresh on frame activation.
                        RepositoryRefresh.after(p, repoPath);

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

        // UC-SHARE-008, Rule-SHARE-040
        private void promptAndSetGitIdentity(final @NotNull Path repoPath, final @NotNull PendingCommitsDialog.Request request, final @NotNull String branch) {
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
                                    performCommitWorkflow(repoPath, request, branch);
                                });
                            },
                            ex -> Services.getInstance(p, Notifier.class).error(p, "Config Failed",
                                    "Failed to set Git identity:" + System.lineSeparator() + ex.getMessage()))
            ).show());
        }

        /**
         * UC-SHARE-008, Rule-SHARE-039.
         * <p>
         * An exception with no message of its own arrives here as the empty string,
         * converted where it comes out of the JDK rather than checked here (#71).
         */
        private boolean isIdentityError(final @NotNull String message) {
            final @NotNull String normalized = message.toLowerCase(Locale.ROOT);
            return normalized.contains("author identity unknown")
                    || normalized.contains("please tell me who you are");
        }
    }
}
