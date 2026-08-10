package org.testin.git;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Remote synchronization operations for a Git repository.
 */
public final class GitSyncService {

    private final @NotNull Project project;

    public GitSyncService(final @NotNull Project project) {
        this.project = project;
    }

    public void pullMain(final @NotNull Path repositoryPath) {
        GitCommandRunner.execute(project, repositoryPath,
                "git", "pull", "--rebase", "--autostash", "origin", "main");
    }
}
