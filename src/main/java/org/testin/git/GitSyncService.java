package org.testin.git;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Remote synchronization operations for a Git repository.
 */
public final class GitSyncService {

    private final @NotNull Project project;
    private final @NotNull GitRepositoryService repositories;

    public GitSyncService(final @NotNull Project project) {
        this.project = project;
        this.repositories = new GitRepositoryService(project);
    }

    public void pull(final @NotNull Path repositoryPath, final @NotNull String remote, final @NotNull String branch) {
        GitCommandRunner.execute(project, repositoryPath,
                "git", "pull", "--rebase", "--autostash", remote, branch);
    }

    public boolean hasConflicts(final @NotNull Path repositoryPath) {
        return repositories.hasConflicts(repositoryPath);
    }

    public void abortRebase(final @NotNull Path repositoryPath) throws com.intellij.openapi.vcs.VcsException {
        repositories.abortRebase(repositoryPath);
    }

    public void continueRebase(final @NotNull Path repositoryPath) throws com.intellij.openapi.vcs.VcsException {
        repositories.continueRebase(repositoryPath);
    }
}
