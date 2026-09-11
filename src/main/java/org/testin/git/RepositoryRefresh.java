package org.testin.git;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import git4idea.GitUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditors;
import org.testin.explorer.TreePanel;
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
     * UC-SHARE-016, Rule-SHARE-073.
     * <p>
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
            // Before the tree, and outside its guard: an editor is open whether
            // or not the Testin tool window ever was, and a pull that changed a
            // test case leaves it showing what was there before.
            //
            // Said by this path rather than left to the file watcher. The watcher
            // would catch it - the VFS refresh above is exactly the event it
            // listens for - but four tenths of a second after the sync says it
            // succeeded, and only for a project whose panel exists. A sync that
            // reports success owns what it changed (#20).
            Services.getInstance(p, TestinEditors.class).refreshOpen(p);

            if (Services.isNotCreated(p, TreePanel.class)) return;

            Services.getInstance(p, TreePanel.class).getProjectTree().refresh();
        });
    }
}
