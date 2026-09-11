package org.testin.git;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.config.ConnectionType;
import org.testin.config.TestinConfigService;
import org.testin.logger.Logger;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;

import java.nio.file.Path;

/**
 * Records the test project's remote in {@code testin.yml}, once (#94).
 * <p>
 * That line is what lets a colleague who clones this repository be offered the
 * test project rather than an empty tree - the tree panel already reads it to do
 * so. Until this existed nothing wrote it unless somebody happened to configure
 * a remote during a push, so the usual case was a repository that knew where its
 * test data lived and never said.
 * <p>
 * Here rather than on the panel that calls it, which is where it was until #291:
 * asking Git for a remote and writing a line into the repository's config are
 * both Git-side work, and a panel that draws a tree was doing them on every
 * refresh.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClonedFrom {

    /**
     * UC-SHARE-014.
     * <p>
     * Off the EDT: asking Git for a remote URL is a command. Written only when
     * the file has nothing, so a URL a team chose deliberately is never replaced
     * by whatever one machine happens to have.
     * <p>
     * The guard is "the file already answered", not "the file answered and is a
     * Git project". Those read alike and are not: a repository reached over SFTP,
     * or one with no location at all, is never a Git project - so the second
     * form can never become true, and this ran again on every refresh, writing
     * the key over and over. A file that says sftp is skipped outright, because
     * a Git URL is not a fact about it.
     */
    public static void record(final @NotNull Project p, final @NotNull Path projectPath) {
        if (!OptionalPlugin.GIT.isAvailable()) return;

        final @NotNull TestinConfigService config = Services.getInstance(p, TestinConfigService.class);
        if (!config.get().repoUrl().isEmpty()) return;
        if (config.get().connection() == ConnectionType.SFTP) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull GitRepositoryService git = new GitRepositoryService(p);
            if (git.isNotRepository(projectPath)) return;

            final @NotNull String remote = git.getRemoteName(projectPath);
            if (remote.isEmpty()) return;

            final @NotNull String url = git.getRemoteUrl(projectPath, remote);
            if (url.isEmpty()) return;

            config.rememberRepoUrl(url);
            Logger.info("Recorded where " + projectPath.getFileName() + " is cloned from: " + url);
        });
    }
}
