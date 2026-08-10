package org.testin.git;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
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
        final Set<String> paths = selectedChanges.stream()
                .map(TestCaseDiff::relativeFilePath)
                .map(path -> path.toString().replace('\\', '/'))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (paths.isEmpty()) throw new IllegalArgumentException("No Git changes were selected");

        GitCommandRunner.execute(project, repositoryPath, withPaths("git", "add", "--", paths));
        GitCommandRunner.execute(project, repositoryPath, withPaths("git", "commit", "--only", "-m", message, paths));
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

    private String[] withPaths(
            final String command,
            final String firstArgument,
            final String secondArgument,
            final Set<String> paths) {
        final String[] result = new String[3 + paths.size()];
        result[0] = command;
        result[1] = firstArgument;
        result[2] = secondArgument;
        int index = 3;
        for (final String path : paths) result[index++] = path;
        return result;
    }

    private String[] withPaths(
            final String command,
            final String firstArgument,
            final String secondArgument,
            final String thirdArgument,
            final String fourthArgument,
            final Set<String> paths) {
        final String[] result = new String[6 + paths.size()];
        result[0] = command;
        result[1] = firstArgument;
        result[2] = secondArgument;
        result[3] = thirdArgument;
        result[4] = fourthArgument;
        result[5] = "--";
        int index = 6;
        for (final String path : paths) result[index++] = path;
        return result;
    }
}
