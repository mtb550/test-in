package org.testin.git;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import git4idea.GitUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.ExplorerPanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Brings the IDE back in line with what Git has just changed on disk.
 * <p>
 * A pull with a rebase writes a colleague's test cases into the working tree
 * behind the IDE's back, so the virtual file system has to be refreshed and the
 * project re-indexed before anything on screen means what it says.
 * <p>
 * One owner because only one of the two Git workflows did it. The sync did; the
 * push from Pending Commits pulls with a rebase and then refreshed nothing, so
 * the tester was looking at pre-pull data under a balloon saying the push had
 * succeeded - and it corrected itself only when the IDE happened to refresh on
 * frame activation, which made the staleness intermittent and impossible to
 * reproduce on purpose.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class RepositoryRefresh {

    /**
     * Refreshes the working tree, re-indexes the project, and redraws the
     * explorer.
     * <p>
     * The tree is only redrawn when the panel already exists. Asking for it
     * would build one, and building one starts indexing in a project whose
     * Testin tool window the tester never opened (#77).
     */
    static void after(final @NotNull Project p, final @NotNull Path repoPath) {
        Optional.ofNullable(LocalFileSystem.getInstance().refreshAndFindFileByIoFile(repoPath.toFile()))
                .ifPresent(GitUtil::refreshVfsInRoot);

        Services.getInstance(p, ProjectIndexer.class).scanSingleProject(repoPath);

        ApplicationManager.getApplication().invokeLater(() -> {
            if (Services.isNotCreated(p, ExplorerPanel.class)) return;

            Services.getInstance(p, ExplorerPanel.class).getProjectTree().refresh();
        });
    }
}
