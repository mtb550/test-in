package org.testin.git;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * The pull half of the sync workflow.
 * <p>
 * It used to forward three more methods - conflicts, abort, continue - straight
 * to {@link GitRepositoryService} without adding anything, so a caller wanting
 * to know about conflicts had two places to ask and no way to tell which was
 * meant. They ask the repository service directly now.
 */
public final class GitSyncService {

    private final @NotNull Project p;

    public GitSyncService(final @NotNull Project p) {
        this.p = p;
    }

    /**
     * Pulls, telling the handler which remote URL it is for.
     * <p>
     * The URL is what lets the IDE find the credentials it already holds for
     * that host. This was the one network command in the plugin that did not
     * pass it, so a sync against a private repository asked for credentials the
     * IDE had already been given, or failed where every other command succeeded.
     */
    public void pull(final @NotNull Path repositoryPath, final @NotNull String remoteUrl, final @NotNull String remote, final @NotNull String branch) {
        GitCommandRunner.executeRemote(p, repositoryPath, remoteUrl,
                "git", "pull", "--rebase", "--autostash", remote, branch);
    }

}
