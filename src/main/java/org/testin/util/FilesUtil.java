package org.testin.util;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.logger.Logger;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service(Service.Level.PROJECT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FilesUtil {

    public <T> void write(final @NotNull Project project, final @NotNull Path path, final @NotNull T content) {
        try {
            byte[] jsonBytes = Services.getInstance(project, Mapper.class).writeValueAsBytes(content);
            Files.write(path, jsonBytes);

        } catch (final IOException ex) {
            Services.getInstance(project, Notifier.class).error(project, "unable to write content: " + ex.getMessage());
            Logger.error("unable to write content: " + ex.getMessage());
            Logger.error("path" + path);
            ex.printStackTrace(System.err);
        }
    }

    public void createDirectories(final @NotNull Path path) {
        try {
            Files.createDirectories(path);
        } catch (final IOException ex) {
            Logger.error("Exception: " + ex.getMessage());
        }
    }

}
