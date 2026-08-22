package org.testin.git;

import com.intellij.openapi.project.Project;
import git4idea.GitUtil;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.List;

/**
 * Repository and branch operations, run as Git commands. The naming and parsing
 * rules live in {@link GitRefs}, which is testable without an IDE or a
 * repository; this class only runs the commands and hands their output over.
 * <p>
 * It used to ask the IDE for a {@code GitRepository} instead, and that could not
 * work here: the IDE only knows repositories registered as VCS roots in the open
 * project, and a Testin root is deliberately a separate repository from the
 * automation project. Every lookup came back null, so the branch was unknown, the
 * branch list was empty, and a push failed with "Could not determine the
 * repository default branch" on a repository whose branch Git could name
 * immediately.
 */
@AllArgsConstructor
public final class GitRepositoryService {

    private final @NotNull Project project;

    /**
     * A filesystem question, not an IDE one - so this stays as it was.
     */
    /**
     * True when the directory is not a Git repository at all.
     * <p>
     * Asked in the negative because that is the only way it is ever asked - all
     * three callers wrote {@code if (!isRepository(...))} - and a question read
     * one way at every call site should be named that way. The same rule
     * {@link #couldNotAbortRebase} follows.
     */
    public boolean isNotRepository(final @NotNull Path path) {
        return !GitUtil.isGitRoot(path);
    }

    /**
     * The checked-out branch, and nothing at all when Git cannot name one - an
     * empty repository before its first commit, or a detached HEAD.
     */
    public @NotNull String getCurrentBranch(final @NotNull Path path) {
        return run(path, "git", "branch", "--show-current").orElse("").trim();
    }

    /**
     * The branch a push should go to: what the remote calls its HEAD, falling
     * back to the branch checked out here.
     */
    public @NotNull String getDefaultBranch(final @NotNull Path path) {
        final @NotNull String currentBranch = getCurrentBranch(path);
        final @NotNull String remoteName = getRemoteName(path);
        if (remoteName.isEmpty()) return currentBranch;

        final @NotNull String headBranch = runRemote(path, getRemoteUrl(path, remoteName), "git", "remote", "show", remoteName)
                .map(GitRefs::parseHeadBranch)
                .orElse("");

        return headBranch.isEmpty() ? currentBranch : headBranch;
    }

    /**
     * The branch this repository syncs: the one that is checked out.
     * <p>
     * The one owner of that question, because it used to have two answers. The
     * push already used the branch its commit went onto; the Sync button asked
     * {@link #getDefaultBranch} instead, which is what the <em>remote</em> calls
     * its HEAD. A tester on a feature branch therefore pressed Sync and had
     * every commit on it replayed onto master - conflicts on work that had none,
     * a rewritten branch that no longer matched its own remote, and their test
     * cases never reaching the branch they were on (#89).
     * <p>
     * The remote's default is the fallback and nothing more, for the two cases
     * where Git cannot name a branch here: a detached HEAD, and a repository
     * with no commit in it yet.
     */
    public @NotNull String syncBranch(final @NotNull Path path) {
        final @NotNull String current = getCurrentBranch(path);

        return current.isEmpty() ? getDefaultBranch(path) : current;
    }

    /**
     * Which commit a running rebase is replaying, counting from one, and zero
     * when no rebase is running.
     * <p>
     * Read so that resolving conflicts one after another can tell progress from
     * a standstill: a round that leaves this where it found it has not moved the
     * rebase on, and repeating it would only ask the same question forever.
     * <p>
     * Git keeps the number in the directory it leaves behind for the rebase -
     * the same directory {@link #isRebaseInProgress} looks for - and the two
     * backends name their file differently.
     */
    public int rebaseStep(final @NotNull Path path) {
        final @NotNull Path gitDir = path.resolve(".git");

        return readStep(gitDir.resolve("rebase-merge").resolve("msgnum"))
                + readStep(gitDir.resolve("rebase-apply").resolve("next"));
    }

    /**
     * The number in one of Git's counter files, and zero when it is not there
     * or holds something else - both of which mean "no rebase to be at a step
     * of".
     */
    private int readStep(final @NotNull Path counter) {
        try {
            return Integer.parseInt(Files.readString(counter).trim());
        } catch (final IOException | NumberFormatException ex) {
            return 0;
        }
    }

    public void fetchRemoteBranches(final @NotNull Path path) {
        final @NotNull String remoteName = getRemoteName(path);
        if (remoteName.isEmpty()) return;

        runRemote(path, getRemoteUrl(path, remoteName), "git", "fetch", "--all", "--prune");
    }

    public @NotNull List<String> getAvailableBranches(final @NotNull Path path) {
        return GitRefs.parseBranches(run(path, "git", "branch", "-a").orElse("").lines().toList());
    }

    /**
     * Empty when there is no such remote, and equally when reading the config
     * failed — both callers already treat an empty URL as "no remote is
     * configured", which is what an unreadable config amounts to.
     */
    public @NotNull String getRemoteUrl(final @NotNull Path path, final @NotNull String remoteName) {
        return run(path, "git", "remote", "get-url", remoteName).orElse("").trim();
    }

    /**
     * The remote to work with, and nothing at all when the repository has none.
     */
    public @NotNull String getRemoteName(final @NotNull Path path) {
        return GitRefs.chooseRemote(run(path, "git", "remote").orElse("").lines().toList());
    }

    /**
     * The branch that is now checked out, and nothing at all when the checkout
     * did not happen — which the caller reads as "put the branch box back where
     * it was". The git reason goes to the log rather than into the caller's
     * balloon: it is command output, and the sentence the tester needs
     * (uncommitted changes) is the caller's to write.
     */
    public @NotNull String checkout(final @NotNull Path path, final @NotNull String branch) {
        final @NotNull String localName = GitRefs.localNameOf(branch);
        final boolean remoteBranch = !localName.equals(branch);

        // A remote branch checked out by its remote name detaches HEAD. Tracking
        // it under its local name is what the tester meant by picking it.
        if (remoteBranch && !getAvailableBranches(path).contains(localName)) {
            return run(path, "git", "checkout", "-b", localName, "--track", branch).isPresent() ? localName : "";
        }

        final @NotNull String target = remoteBranch ? localName : branch;
        return run(path, "git", "checkout", target).isPresent() ? target : "";
    }

    /**
     * Starts a branch at the current commit and moves onto it, answering whether
     * it worked.
     * <p>
     * Uncommitted work comes along, which is the point: the tester is in the
     * review because they have changes, and naming a new branch there means they
     * want those changes on it. That is also why this never refuses the way a
     * plain checkout does - there is nothing to overwrite on a branch that did
     * not exist a moment ago.
     */
    public boolean startBranch(final @NotNull Path path, final @NotNull String branch) {
        return run(path, "git", "checkout", "-b", branch).isPresent();
    }

    /**
     * The branches on this machine, current one first. What the review offers to
     * commit onto - remote-only branches are left out, because committing onto
     * one means creating the local branch anyway, and the tester can type the
     * name to do exactly that.
     */
    public @NotNull List<String> getLocalBranches(final @NotNull Path path) {
        return GitRefs.parseBranches(run(path, "git", "branch").orElse("").lines().toList());
    }

    /**
     * What Git itself reports as changed, one porcelain line per file.
     * <p>
     * {@code -uall} rather than the default: without it Git collapses an
     * untracked directory into a single entry, so a new test set arrives as
     * "Test Cases/login/" and not one line per test case in it.
     */
    public @NotNull List<String> status(final @NotNull Path path) {
        return run(path, "git", "status", "--porcelain", "-uall").orElse("")
                .lines().filter(line -> !line.isBlank()).toList();
    }

    /**
     * A file's content as committed, and empty when there is no committed
     * version to read - the file is new, or the repository has no commits at
     * all, which is every repository on the day it is initialized.
     */
    public @NotNull String showAtHead(final @NotNull Path path, final @NotNull String relativePath) {
        return run(path, "git", "show", "HEAD:" + relativePath).orElse("");
    }

    /**
     * True while a pull has stopped on a conflict: either a rebase is halfway
     * through, or Git reports a path both sides touched.
     */
    public boolean hasConflicts(final @NotNull Path path) {
        if (isRebaseInProgress(path)) return true;

        return GitRefs.hasUnmergedPaths(run(path, "git", "status", "--porcelain").orElse("").lines().toList());
    }

    /**
     * One side of a conflicted file as Git holds it in the index.
     * <p>
     * The three sides of a conflict never touch the working tree, which holds
     * the marked-up mixture instead - so a merge that means to read them reads
     * them from here. Empty when that stage does not exist, which is what two
     * testers creating the same file leaves behind: no common ancestor (#90).
     *
     * @param stage 1 for the common ancestor, 2 for the branch being replayed
     *              onto, 3 for the commits being replayed
     */
    public @NotNull String stageContent(final @NotNull Path path, final @NotNull String relativePath, final int stage) {
        return run(path, "git", "show", ":" + stage + ":" + relativePath).orElse("");
    }

    /**
     * Stages a file whose conflict has been resolved, which is how Git is told
     * it is over. Answers whether it worked - a resolution Git did not accept
     * would stop the rebase again with nothing left on screen to explain it.
     */
    public boolean stageResolved(final @NotNull Path path, final @NotNull String relativePath) {
        return run(path, "git", "add", "--", relativePath).isPresent();
    }

    /**
     * The files a stopped pull left conflicting, in the order Git lists them.
     */
    public @NotNull List<String> conflictingPaths(final @NotNull Path path) {
        return GitRefs.unmergedPaths(status(path));
    }

    /**
     * How many commits are on this branch and not on its remote.
     * <p>
     * The number nobody could see: a commit that succeeded and a push that
     * failed leave a repository with nothing pending and work that has not left
     * the machine, which read as "nothing to commit" and no way forward.
     * <p>
     * Zero when Git cannot answer - a branch with no upstream that the remote
     * has never heard of has nothing to be ahead of.
     */
    public int unpushedCount(final @NotNull Path path) {
        final @NotNull String counted = run(path, "git", "rev-list", "--count", "@{upstream}..HEAD").orElse("").trim();
        if (counted.isEmpty()) return 0;

        try {
            return Integer.parseInt(counted);
        } catch (final NumberFormatException ex) {
            Logger.debug("Could not read the unpushed count in " + path + ": " + counted);
            return 0;
        }
    }

    /**
     * Git leaves one of these directories behind for the duration of a rebase -
     * which is how Git itself knows, and the only way to ask without a
     * repository object.
     */
    private boolean isRebaseInProgress(final @NotNull Path path) {
        final @NotNull Path gitDir = path.resolve(".git");
        return Files.isDirectory(gitDir.resolve("rebase-merge")) || Files.isDirectory(gitDir.resolve("rebase-apply"));
    }

    /**
     * True when the abort did not happen. Asked in the negative because that is
     * the only way it was ever asked - both callers wrote {@code if (!abort...)}
     * - and a question read one way at every call site should be named that way.
     * <p>
     * The caller decides what to say: a rebase that still has conflicts is
     * re-offered rather than reported as a plain failure, and only the caller
     * knows that.
     */
    public boolean couldNotAbortRebase(final @NotNull Path path) {
        return run(path, "git", "rebase", "--abort").isEmpty();
    }

    /**
     * True when the rebase did not continue — see {@link #couldNotAbortRebase}.
     */
    public boolean couldNotContinueRebase(final @NotNull Path path) {
        return run(path, "git", "rebase", "--continue").isEmpty();
    }

    /**
     * Runs a local command and answers nothing when it failed, rather than
     * raising.
     * <p>
     * Every caller here treats a failure as "no answer" - there is no branch, no
     * remote, the rebase did not continue - and each of them already answers
     * empty or false for it. The reason goes to the log, where a git message
     * belongs; the sentence the tester reads is written by whoever called.
     * <p>
     * An Optional rather than an empty string, because a command that succeeds
     * with no output is not a command that failed: {@code git checkout} prints
     * nothing on success, and reading that as a failure would leave the branch
     * box showing a branch nobody is on (#71).
     */
    private @NotNull Optional<String> run(final @NotNull Path path, final @NotNull String... command) {
        return execute(path, "", command);
    }

    /**
     * The same, for a command that reaches the remote: the URL is what makes
     * {@code git4idea} set up authentication for it, so a fetch or a push can
     * ask for credentials. See {@code GitCommandRunner.executeRemote}.
     */
    private @NotNull Optional<String> runRemote(final @NotNull Path path, final @NotNull String remoteUrl, final @NotNull String... command) {
        return execute(path, remoteUrl, command);
    }

    /**
     * The one body both entry points share: a blank URL is a local command.
     */
    private @NotNull Optional<String> execute(final @NotNull Path path, final @NotNull String remoteUrl, final @NotNull String... command) {
        try {
            return Optional.of(GitCommandRunner.executeRemote(project, path, remoteUrl, command));
        } catch (final RuntimeException ex) {
            Logger.debug("git " + String.join(" ", command) + " failed in " + path + ": " + ex.getMessage());
            return Optional.empty();
        }
    }
}
