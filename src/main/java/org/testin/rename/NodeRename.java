package org.testin.rename;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Renamed;
import org.testin.explorer.ExplorerPanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.services.Services;
import org.testin.util.EditorUtil;
import org.testin.util.OptionalPlugin;

import java.nio.file.Path;

/**
 * Renaming a node, wherever the rename was asked for.
 * <p>
 * The tree's Rename action asks for it, and so does editing a test run, which
 * lets a tester change the run's name alongside the cases it covers (#96). One
 * routine for both, because the order here is not obvious and every way of
 * getting it wrong is quiet: the editor has to close before the node moves or it
 * sits there holding data that has just been renamed, the generated code is
 * found by the <b>old</b> name so codegen runs first, and the tree refreshes
 * only after the indexer has finished - refreshing earlier shows stale state.
 * <p>
 * Deliberately without the undo entry. The two callers record different things -
 * a rename on its own, and a rename that is one half of an edit - and an undo
 * pushed here would give the second caller two entries for one gesture.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NodeRename {

    /**
     * The callback runs when the rename has finished and the tree has caught up,
     * never if it failed - {@code renameNode} reports and swallows that.
     */
    public static void apply(final @NotNull Project p, final @NotNull ExplorerPanel pp, final @NotNull DirectoryDto dir, final @NotNull String newName, final @NotNull Runnable onDone) {
        Services.getInstance(p, EditorUtil.class).close(p, dir);

        // Before the data rename, while the old name is still what finds the
        // generated code. Which generator that is belongs to the node, not here.
        if (OptionalPlugin.JAVA.isAvailableOrWarnOnce(p)) {
            dir.getType().getRenameCodegen().execute(p, new Renamed(dir, newName));
        }

        final @NotNull Path oldPath = dir.getPath();
        final @NotNull Path newPath = oldPath.getParent().resolve(newName);

        // The tree refreshes only after the indexer finished the VFS rename
        // and updated its cache - refreshing earlier shows stale state.
        Services.getInstance(p, ProjectIndexer.class).renameNode(oldPath, newPath, () -> {
            pp.getProjectTree().refresh();

            if (dir instanceof TestProjectDirectoryDto) {
                pp.refresh();
            }

            Logger.info("Success! Renamed to: " + newName);

            onDone.run();
        });
    }
}
