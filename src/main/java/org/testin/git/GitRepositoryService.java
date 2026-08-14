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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Repository lookup and branch operations backed by IntelliJ Git4Idea. The naming
 * and selection rules themselves live in {@link GitRefs}, which is testable
 * without an IDE; this class only runs the commands and hands their output over.
 */
public final class GitRepositoryService {

    private final @NotNull Project project;

    public GitRepositoryService(final @NotNull Project project) {
        this.project = project;
    }

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

    public @NotNull String getRemoteUrl(final @NotNull Path path, final @NotNull String remoteName) throws VcsException {
        final VirtualFile root = LocalFileSystem.getInstance().findFileByNioFile(path);
        if (root == null) return "";
        final String remote = git4idea.config.GitConfigUtil.getValue(project, root, "remote." + remoteName + ".url");
        return remote == null ? "" : remote.trim();
    }

    public @Nullable String getRemoteName(final @NotNull Path path) {
        try {
            return GitRefs.chooseRemote(GitCommandRunner.execute(project, path, "git", "remote").lines().toList());
        } catch (final RuntimeException ignored) {
            return null;
        }
    }

    public @NotNull String checkout(final @NotNull Path path, final @NotNull String branch) throws VcsException {
        final GitRepository repository = requireRepository(path);
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
    }

    public boolean hasConflicts(final @NotNull Path path) {
        final GitRepository repository = findRepository(path);
        if (repository == null) return false;
        if (repository.isRebaseInProgress()) return true;
        final GitCommandResult result = Git.getInstance().getUnmergedFiles(repository);
        return result.success() && !result.getOutput().isEmpty();
    }

    public void abortRebase(final @NotNull Path path) throws VcsException {
        final GitRepository repository = requireRepository(path);
        Git.getInstance().rebaseAbort(repository).throwOnError();
    }

    public void continueRebase(final @NotNull Path path) throws VcsException {
        final GitRepository repository = requireRepository(path);
        Git.getInstance().rebaseContinue(repository).throwOnError();
    }

    private @NotNull GitRepository requireRepository(final @NotNull Path path) throws VcsException {
        final GitRepository repository = findRepository(path);
        if (repository == null) {
            throw new VcsException("Git repository not found: " + path);
        }
        return repository;
    }
}
