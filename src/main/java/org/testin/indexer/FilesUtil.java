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

    private void reportWriteFailure(final @NotNull Project p, final @NotNull Path path, final @NotNull IOException ex) {
        Services.getInstance(p, Notifier.class).error(p, "unable to write content: " + ex.getMessage());
        Logger.error("unable to write content: " + ex.getMessage());
        Logger.error("path" + path);
    }


}
