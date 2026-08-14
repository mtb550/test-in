package org.testin.util;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service(Service.Level.PROJECT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FilesUtil {

    public <T> void write(final @NotNull Project p, final @NotNull Path path, final @NotNull T content) {
        try {
            writeBytes(path, Services.getInstance(p, Mapper.class).writeValueAsBytes(content));
        } catch (final IOException ex) {
            reportWriteFailure(p, path, ex);
        }
    }

    /**
     * Writes pre-serialized JSON. Used by the run-status writer, which snapshots
     * the bytes on the EDT and performs only the disk I/O on its worker thread.
     */
    public void write(final @NotNull Project p, final @NotNull Path path, final byte @NotNull [] jsonBytes) {
        try {
            writeBytes(path, jsonBytes);
        } catch (final IOException ex) {
            reportWriteFailure(p, path, ex);
        }
    }

    private void writeBytes(final @NotNull Path path, final byte @NotNull [] jsonBytes) throws IOException {
        final @Nullable Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, jsonBytes);
    }

    private void reportWriteFailure(final @NotNull Project p, final @NotNull Path path, final @NotNull IOException ex) {
        Services.getInstance(p, Notifier.class).error(p, "unable to write content: " + ex.getMessage());
        Logger.error("unable to write content: " + ex.getMessage());
        Logger.error("path" + path);
    }


}
