package org.testin.git;

import com.intellij.openapi.project.Project;
import git4idea.GitUtil;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
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
    public boolean isRepository(final @NotNull Path path) {
        return GitUtil.isGitRoot(path);
    }

    /**
     * The checked-out branch, or null when Git cannot name one - an empty
     * repository before its first commit, or a detached HEAD.
     */
    public @Nullable String getCurrentBranch(final @NotNull Path path) {
        final String branch = run(path, "git", "branch", "--show-current");
        return branch == null || branch.isBlank() ? null : branch.trim();
    }

    /**
     * The branch a push should go to: what the remote calls its HEAD, falling
     * back to the branch checked out here.
     */
    public @Nullable String getDefaultBranch(final @NotNull Path path) {
        final String currentBranch = getCurrentBranch(path);
        final String remoteName = getRemoteName(path);
        if (remoteName == null) return currentBranch;

        final String remoteInfo = runRemote(path, getRemoteUrl(path, remoteName), "git", "remote", "show", remoteName);
        final String headBranch = remoteInfo == null ? null : GitRefs.parseHeadBranch(remoteInfo);
        return headBranch != null ? headBranch : currentBranch;
    }

    public void fetchRemoteBranches(final @NotNull Path path) {
        final String remoteName = getRemoteName(path);
        if (remoteName == null) return;

        runRemote(path, getRemoteUrl(path, remoteName), "git", "fetch", "--all", "--prune");
    }

    public @NotNull List<String> getAvailableBranches(final @NotNull Path path) {
        final String output = run(path, "git", "branch", "-a");
        return output == null ? List.of() : GitRefs.parseBranches(output.lines().toList());
    }

    /**
     * Empty when there is no such remote, and equally when reading the config
     * failed — both callers already treat an empty URL as "no remote is
     * configured", which is what an unreadable config amounts to.
     */
    public @NotNull String getRemoteUrl(final @NotNull Path path, final @NotNull String remoteName) {
        final String url = run(path, "git", "remote", "get-url", remoteName);
        return url == null ? "" : url.trim();
    }

    public @Nullable String getRemoteName(final @NotNull Path path) {
        final String output = run(path, "git", "remote");
        return output == null ? null : GitRefs.chooseRemote(output.lines().toList());
    }

    /**
     * The branch that is now checked out, or null when the checkout failed —
     * which the caller reads as "put the branch box back where it was". The git
     * reason goes to the log rather than into the caller's balloon: it is
     * command output, and the sentence the tester needs (uncommitted changes)
     * is the caller's to write.
     */
    public @Nullable String checkout(final @NotNull Path path, final @NotNull String branch) {
        final String localName = GitRefs.localNameOf(branch);
        final boolean remoteBranch = !localName.equals(branch);

        // A remote branch checked out by its remote name detaches HEAD. Tracking
        // it under its local name is what the tester meant by picking it.
        if (remoteBranch && !getAvailableBranches(path).contains(localName)) {
            return run(path, "git", "checkout", "-b", localName, "--track", branch) == null ? null : localName;
        }

        final String target = remoteBranch ? localName : branch;
        return run(path, "git", "checkout", target) == null ? null : target;
    }

    /**
     * What Git itself reports as changed, one porcelain line per file.
     * <p>
     * {@code -uall} rather than the default: without it Git collapses an
     * untracked directory into a single entry, so a new test set arrives as
     * "Test Cases/login/" and not one line per test case in it.
     */
    public @NotNull List<String> status(final @NotNull Path path) {
        final String output = run(path, "git", "status", "--porcelain", "-uall");
        return output == null ? List.of() : output.lines().filter(line -> !line.isBlank()).toList();
    }

    /**
     * A file's content as committed, or null when there is no committed version
     * to read - the file is new, or the repository has no commits at all, which
     * is every repository on the day it is initialized.
     */
    public @Nullable String showAtHead(final @NotNull Path path, final @NotNull String relativePath) {
        return run(path, "git", "show", "HEAD:" + relativePath);
    }

    /**
     * True while a pull has stopped on a conflict: either a rebase is halfway
     * through, or Git reports a path both sides touched.
     */
    public boolean hasConflicts(final @NotNull Path path) {
        if (isRebaseInProgress(path)) return true;

        final String output = run(path, "git", "status", "--porcelain");
        return output != null && GitRefs.hasUnmergedPaths(output.lines().toList());
    }

    /**
     * Git leaves one of these directories behind for the duration of a rebase -
     * which is how Git itself knows, and the only way to ask without a
     * repository object.
     */
    private boolean isRebaseInProgress(final @NotNull Path path) {
        final Path gitDir = path.resolve(".git");
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
        return run(path, "git", "rebase", "--abort") == null;
    }

    /**
     * True when the rebase did not continue — see {@link #couldNotAbortRebase}.
     */
    public boolean couldNotContinueRebase(final @NotNull Path path) {
        return run(path, "git", "rebase", "--continue") == null;
    }

    /**
     * Runs a local command and answers null when it failed, rather than raising.
     * <p>
     * Every caller here treats a failure as "no answer" - there is no branch, no
     * remote, the rebase did not continue - and each of them already returns
     * null or false for it. The reason goes to the log, where a git message
     * belongs; the sentence the tester reads is written by whoever called.
     */
    private @Nullable String run(final @NotNull Path path, final @NotNull String... command) {
        return execute(path, "", command);
    }

    /**
     * The same, for a command that reaches the remote: the URL is what makes
     * {@code git4idea} set up authentication for it, so a fetch or a push can
     * ask for credentials. See {@code GitCommandRunner.executeRemote}.
     */
    private @Nullable String runRemote(final @NotNull Path path, final @NotNull String remoteUrl, final @NotNull String... command) {
        return execute(path, remoteUrl, command);
    }

    /**
     * The one body both entry points share: a blank URL is a local command.
     */
    private @Nullable String execute(final @NotNull Path path, final @NotNull String remoteUrl, final @NotNull String... command) {
        try {
            return GitCommandRunner.executeRemote(project, path, remoteUrl, command);
        } catch (final RuntimeException ex) {
            Logger.debug("git " + String.join(" ", command) + " failed in " + path + ": " + ex.getMessage());
            return null;
        }
    }
}
