package org.testin.util;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.logger.Log;
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

        } catch (IOException e) {
            Services.getInstance(project, Notifier.class).error(project, "unable to write content: " + e.getMessage());
            Log.error("unable to write content: " + e.getMessage());
            Log.error("path" + path);
            e.printStackTrace(System.err);
        }
    }

    public <T> void write2(final @NotNull Project project, final @NotNull VirtualFile vf, final @NotNull T content) {
        try {
            byte[] jsonContent = Services.getInstance(project, Mapper.class).writeValueAsBytes(content);
            vf.setBinaryContent(jsonContent);

        } catch (IOException e) {
            Services.getInstance(project, Notifier.class).error(project, "unable to write content: " + e.getMessage());
            Log.error("unable to write content: " + e.getMessage());
            Log.error("vf: " + vf.getPath());
            e.printStackTrace(System.err);
        }
    }

    public void createDirectories(final @NotNull Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            Log.error("Exception: " + e.getMessage());
        }
    }

}
