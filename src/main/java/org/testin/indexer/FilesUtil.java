package org.testin.indexer;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
        try {
            final @Nullable Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
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
