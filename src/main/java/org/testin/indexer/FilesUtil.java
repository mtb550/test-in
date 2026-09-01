package org.testin.indexer;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Writes test data to disk. Package-private, and in this package, so that the
 * architecture rule is enforced by the compiler rather than by convention: the
 * indexer is the single owner of test data file access, and nothing outside it
 * can reach this writer at all.
 */
@Service(Service.Level.PROJECT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FilesUtil {

    <T> void write(final @NotNull Project p, final @NotNull Path path, final @NotNull T content) {
        writeBytes(p, path, Services.getInstance(p, Mapper.class).writeValueAsBytes(content));
    }

    /**
     * Writes pre-serialized JSON. Used by the run-status writer, which snapshots
     * the bytes on the EDT and performs only the disk I/O on its worker thread.
     */
    void write(final @NotNull Project p, final @NotNull Path path, final byte @NotNull [] jsonBytes) {
        writeBytes(p, path, jsonBytes);
    }

    private void writeBytes(final @NotNull Project p, final @NotNull Path path, final byte @NotNull [] jsonBytes) {
        // The last line of defense for test data: writing nothing over a file
        // empties it, and an empty marker takes its node's audit info with it.
        // Six markers in a real data root were left at zero bytes this way.
        // Nothing legitimate written here is empty - the smallest marker is a
        // pair of braces.
        if (jsonBytes.length == 0) {
            Logger.error("Refusing to write an empty file, which would erase it: " + path);
            Services.getInstance(p, Notifier.class).error(p, "Nothing was written to " + path.getFileName());
            return;
        }

        try {
            // Before the write, not after: the VFS event can arrive while this
            // thread is still in Files.write, and a file claimed a moment too
            // late looks to the watcher like somebody else's edit (#20).
            Services.getInstance(OwnWrites.class).record(path);

            // The platform's own helper: it knows that a path with no parent - a
            // filesystem root - has no folder to create.
            FileUtil.createParentDirs(path.toFile());
            Files.write(path, jsonBytes);
        } catch (final IOException ex) {
            reportWriteFailure(p, path, ex);
        }
    }

    /**
     * Removes a file, and the folders it leaves empty behind it, up to but never
     * including the project.
     * <p>
     * A sync that deletes the last case in a test set would otherwise leave the
     * folder and its marker standing, so the tree keeps showing a set with
     * nothing in it that nobody can explain. Stops at the project because an
     * empty project is still a project - somebody made it deliberately.
     */
    void delete(final @NotNull Project p, final @NotNull Path path, final @NotNull Path stopAt) {
        try {
            Services.getInstance(OwnWrites.class).record(path);

            // To the recycle bin, so a case removed by mistake is recovered the
            // way every other file on this machine is. One JSON file, so the
            // move is cheap enough for the thread the removal already runs on.
            if (!Trash.accepted(path)) Files.deleteIfExists(path);
        } catch (final IOException ex) {
            Services.getInstance(p, Notifier.class).error(p, "unable to remove: " + ex.getMessage());
            Logger.error("unable to remove " + path + ": " + ex.getMessage());
            return;
        }

        removeEmptyFolders(path.getParent(), stopAt);
    }

    /**
     * Walks up from a removed file, dropping every folder it emptied.
     * <p>
     * A folder that will not be read is left standing rather than reported: the
     * file the tester asked to remove is already gone, and a leftover empty
     * folder is untidy where a failure notification about it would be alarming.
     */
    private void removeEmptyFolders(final @NotNull Path folder, final @NotNull Path stopAt) {
        for (Path at = folder; at != null && at.startsWith(stopAt) && !at.equals(stopAt); at = at.getParent()) {
            try (Stream<Path> inside = Files.list(at)) {
                if (inside.findAny().isPresent()) return;

                Services.getInstance(OwnWrites.class).record(at);

                // Deleted outright, not trashed: this folder is empty by the
                // time it is reached, so there is nothing in the bin to recover
                // and every removal would leave one there to tidy up.
                Files.deleteIfExists(at);
            } catch (final IOException ex) {
                Logger.warn("Left an empty folder behind at " + at + ": " + ex.getMessage());
                return;
            }
        }
    }

    private void reportWriteFailure(final @NotNull Project p, final @NotNull Path path, final @NotNull IOException ex) {
        Services.getInstance(p, Notifier.class).error(p, "unable to write content: " + ex.getMessage());
        Logger.error("unable to write content: " + ex.getMessage());
        Logger.error("path" + path);
    }


}
