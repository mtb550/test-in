package org.testin.git;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitLocalBranch;
import git4idea.GitRemoteBranch;
import git4idea.GitUtil;
import git4idea.commands.Git;
import git4idea.commands.GitCommandResult;
import git4idea.repo.GitRepository;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Repository lookup and branch operations backed by IntelliJ Git4Idea. The naming
 * and selection rules themselves live in {@link GitRefs}, which is testable
 * without an IDE; this class only runs the commands and hands their output over.
 */
@AllArgsConstructor
public final class GitRepositoryService {

    private final @NotNull Project project;

    public boolean isRepository(final @NotNull Path path) {
        return GitUtil.isGitRoot(path);
    }

    public @Nullable GitRepository findRepository(final @NotNull Path path) {
        final VirtualFile root = LocalFileSystem.getInstance().findFileByNioFile(path);
        if (root == null || !isRepository(path)) return null;
        try {
            return GitUtil.getRepositoryForRoot(project, root);
        } catch (final VcsException ignored) {
            return null;
        }
    }

    public @Nullable String getCurrentBranch(final @NotNull Path path) {
        final GitRepository repository = findRepository(path);
        final GitLocalBranch branch = repository == null ? null : repository.getCurrentBranch();
        return branch == null ? null : branch.getName();
    }

    public @Nullable String getDefaultBranch(final @NotNull Path path) {
        final String currentBranch = getCurrentBranch(path);
        final String remoteName = getRemoteName(path);
        if (remoteName == null) return currentBranch;
        try {
            final String remoteInfo = GitCommandRunner.execute(project, path, "git", "remote", "show", remoteName);
            final String headBranch = GitRefs.parseHeadBranch(remoteInfo);
            return headBranch != null ? headBranch : currentBranch;
        } catch (final RuntimeException ignored) {
            return currentBranch;
        }
    }

    public void fetchRemoteBranches(final @NotNull Path path) {
        if (getRemoteName(path) == null) return;
        GitCommandRunner.execute(project, path, "git", "fetch", "--all", "--prune");
        final GitRepository repository = findRepository(path);
        if (repository != null) GitUtil.updateRepositories(List.of(repository));
    }

    public @NotNull List<String> getAvailableBranches(final @NotNull Path path) {
        final GitRepository repository = findRepository(path);
        if (repository == null) return List.of();

        final Set<String> branches = new LinkedHashSet<>();
        repository.getBranches().getLocalBranches().stream()
                .map(GitLocalBranch::getName)
                .forEach(branches::add);
        repository.getBranches().getRemoteBranches().stream()
                .map(GitRemoteBranch::getName)
                .filter(name -> !name.endsWith("/HEAD"))
                .forEach(branches::add);

        return branches.stream().sorted().toList();
    }

    /**
     * Empty when there is no such remote, and equally when reading the config
     * failed — both callers already treat an empty URL as "no remote is
     * configured", which is what an unreadable config amounts to.
     */
    public @NotNull String getRemoteUrl(final @NotNull Path path, final @NotNull String remoteName) {
        final VirtualFile root = LocalFileSystem.getInstance().findFileByNioFile(path);
        if (root == null) return "";
        try {
            final String remote = git4idea.config.GitConfigUtil.getValue(project, root, "remote." + remoteName + ".url");
            return remote == null ? "" : remote.trim();
        } catch (final VcsException ex) {
            Logger.error("Could not read remote." + remoteName + ".url: " + ex.getMessage());
            return "";
        }
    }

    public @Nullable String getRemoteName(final @NotNull Path path) {
        try {
            return GitRefs.chooseRemote(GitCommandRunner.execute(project, path, "git", "remote").lines().toList());
        } catch (final RuntimeException ignored) {
            return null;
        }
    }

    /**
     * The branch that is now checked out, or null when the checkout failed —
     * which the caller reads as "put the branch box back where it was". The git
     * reason goes to the log rather than into the caller's balloon: it is
     * command output, and the sentence the tester needs (uncommitted changes)
     * is the caller's to write.
     */
    public @Nullable String checkout(final @NotNull Path path, final @NotNull String branch) {
        final GitRepository repository = findRepository(path);
        if (repository == null) {
            Logger.error("Checkout failed, no Git repository at: " + path);
            return null;
        }

        try {
            final boolean remoteBranch = repository.getBranches().getRemoteBranches().stream()
                    .map(GitRemoteBranch::getName)
                    .anyMatch(branch::equals);
            if (remoteBranch) {
                final String localBranch = GitRefs.localNameOf(branch);
                if (repository.getBranches().findLocalBranch(localBranch) == null) {
                    GitCommandRunner.execute(project, path, "git", "checkout", "-b", localBranch, "--track", branch);
                    return localBranch;
                }
            }

            final GitCommandResult result = Git.getInstance().checkout(repository, branch, null, false, false);
            result.throwOnError();
            return branch;

        } catch (final VcsException | RuntimeException ex) {
            Logger.error("Checkout of '" + branch + "' failed: " + ex.getMessage());
            return null;
        }
    }

    public boolean hasConflicts(final @NotNull Path path) {
        final GitRepository repository = findRepository(path);
        if (repository == null) return false;
        if (repository.isRebaseInProgress()) return true;
        final GitCommandResult result = Git.getInstance().getUnmergedFiles(repository);
        return result.success() && !result.getOutput().isEmpty();
    }

    /**
     * False when the abort did not happen. The caller decides what to say: a
     * rebase that still has conflicts is re-offered rather than reported as a
     * plain failure, and only this method's caller knows that.
     */
    public boolean abortRebase(final @NotNull Path path) {
        return rebase(path, "abort", repository -> Git.getInstance().rebaseAbort(repository));
    }

    /**
     * False when the rebase did not continue — see {@link #abortRebase}.
     */
    public boolean continueRebase(final @NotNull Path path) {
        return rebase(path, "continue", repository -> Git.getInstance().rebaseContinue(repository));
    }

    private boolean rebase(final @NotNull Path path, final @NotNull String operation,
                           final @NotNull Function<GitRepository, GitCommandResult> command) {
        final GitRepository repository = findRepository(path);
        if (repository == null) {
            Logger.error("Rebase " + operation + " failed, no Git repository at: " + path);
            return false;
        }

        try {
            command.apply(repository).throwOnError();
            return true;
        } catch (final VcsException | RuntimeException ex) {
            Logger.error("Rebase " + operation + " failed: " + ex.getMessage());
            return false;
        }
    }
}

