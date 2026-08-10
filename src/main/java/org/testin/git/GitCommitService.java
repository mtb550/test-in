package org.testin.git;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.nio.file.Path;

/**
 * Local Git repository operations used by the pending-commit workflow.
 */
public final class GitCommitService {

    private final @NotNull Project project;

    public GitCommitService(final @NotNull Project project) {
        this.project = project;
    }

    public void initialize(final @NotNull Path repositoryPath) {
        GitCommandRunner.execute(project, repositoryPath, "git", "init", "-b", "main");
    }

    public void stageAndCommit(final @NotNull Path repositoryPath, final @NotNull String message) {
        GitCommandRunner.execute(project, repositoryPath, "git", "add", "--all");
        GitCommandRunner.execute(project, repositoryPath, "git", "commit", "-m", message);
    }

    public void configureRemote(final @NotNull Path repositoryPath, final @NotNull String remoteUrl) {
        GitCommandRunner.execute(project, repositoryPath, "git", "remote", "add", "origin", remoteUrl);
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

    public void pullAndPushMain(final @NotNull Path repositoryPath) {
        GitCommandRunner.execute(project, repositoryPath, "git", "branch", "-M", "main");
        GitCommandRunner.execute(project, repositoryPath, "git", "pull", "--rebase", "--autostash", "origin", "main");
        GitCommandRunner.execute(project, repositoryPath, "git", "push", "-u", "origin", "main");
        Logger.info("Git push completed for " + repositoryPath);
    }
}
