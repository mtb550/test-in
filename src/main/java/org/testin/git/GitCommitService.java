package org.testin.git;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * Local Git repository operations used by the pending-commit workflow.
 */
public final class GitCommitService {

    private final @NotNull Project project;

    public GitCommitService(final @NotNull Project project) {
        this.project = project;
    }

    public void initialize(final @NotNull Path repositoryPath) {
        GitCommandRunner.execute(project, repositoryPath, "git", "init");
    }

    public void stageAndCommit(
            final @NotNull Path repositoryPath,
            final @NotNull String message,
            final @NotNull Collection<TestCaseDiff> selectedChanges) {
        final Set<String> paths = GitRefs.repoRelativePaths(selectedChanges);
        if (paths.isEmpty()) throw new IllegalArgumentException("No Git changes were selected");

        GitCommandRunner.execute(project, repositoryPath, withPaths(paths, "git", "add", "--"));
        GitCommandRunner.execute(project, repositoryPath, withPaths(paths, "git", "commit", "--only", "-m", message, "--"));
    }

    public void configureRemote(final @NotNull Path repositoryPath, final @NotNull String remoteName, final @NotNull String remoteUrl) {
        GitCommandRunner.execute(project, repositoryPath, "git", "remote", "add", remoteName, remoteUrl);
    }

    public void configureIdentity(
            final @NotNull Path repositoryPath,
            final @NotNull String name,
            final @NotNull String email,
            final boolean global) {
        final String scope = global ? "--global" : "--local";
        GitCommandRunner.execute(project, repositoryPath, "git", "config", scope, "user.name", name);
        GitCommandRunner.execute(project, repositoryPath, "git", "config", scope, "user.email", email);
    }

    public void pullAndPush(final @NotNull Path repositoryPath, final @NotNull String remote, final @NotNull String branch) {
        GitCommandRunner.execute(project, repositoryPath, "git", "pull", "--rebase", "--autostash", remote, branch);
        push(repositoryPath, remote, branch);
    }

    public void push(final @NotNull Path repositoryPath, final @NotNull String remote, final @NotNull String branch) {
        GitCommandRunner.execute(project, repositoryPath, "git", "push", "-u", remote, branch);
        Logger.info("Git push completed for " + repositoryPath);
    }

    /**
     * Appends the path set after the fixed arguments. Call sites include the
     * {@code "--"} separator explicitly so the full command stays readable.
     */
    private @NotNull String[] withPaths(final @NotNull Set<String> paths, final @NotNull String... fixedArgs) {
        final String[] result = Arrays.copyOf(fixedArgs, fixedArgs.length + paths.size());
        int index = fixedArgs.length;
        for (final String path : paths) result[index++] = path;
        return result;
    }
}
