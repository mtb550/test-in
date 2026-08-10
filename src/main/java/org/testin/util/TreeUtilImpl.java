package org.testin.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.IVfsBiOperation;
import org.testin.enums.IVfsOperation;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.io.IOException;
import java.nio.file.Path;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class TreeUtilImpl {

    public void executeVfsAction(final @NotNull Project p, final @NotNull Path path, final @NotNull String errorTitle, final @NotNull IVfsOperation operation) {
        ApplicationManager.getApplication().invokeLater(() -> WriteAction.run(() -> {
            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
            if (vf != null) {
                operation.execute(vf);
            } else {
                Services.getInstance(p, Notifier.class).error(p, "Could not find path on disk:\n" + path, errorTitle);
            }
        }));
    }

    public void executeVfsAction(final @NotNull Project p, final @NotNull Path sourcePath, final @NotNull Path targetPath, final @NotNull String errorTitle, final @NotNull IVfsBiOperation operation) {
        executeVfsAction(p, sourcePath, targetPath, errorTitle, operation, null);
    }

    public void executeVfsAction(final @NotNull Project p, final @NotNull Path sourcePath, final @NotNull Path targetPath,
                                 final @NotNull String errorTitle, final @NotNull IVfsBiOperation operation,
                                 final Runnable onSuccess) {
        executeVfsAction(p, sourcePath, targetPath, errorTitle, operation, onSuccess, null);
    }

    public void executeVfsAction(final @NotNull Project p, final @NotNull Path sourcePath, final @NotNull Path targetPath,
                                 final @NotNull String errorTitle, final @NotNull IVfsBiOperation operation,
                                 final Runnable onSuccess, final Runnable onFailure) {
        ApplicationManager.getApplication().invokeLater(() -> WriteAction.run(() -> {
            try {
                VirtualFile sourceVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(sourcePath);
                VirtualFile targetVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(targetPath);

                if (sourceVf != null && targetVf != null) {
                    operation.execute(sourceVf, targetVf);
                    if (onSuccess != null) onSuccess.run();
                } else {
                    Services.getInstance(p, Notifier.class).error(p, "Could not find source or target path on disk.", errorTitle);
                    if (onFailure != null) onFailure.run();
                }
            } catch (final Exception ex) {
                Services.getInstance(p, Notifier.class).error(p, "Operation failed: " + ex.getMessage(), errorTitle);
                if (onFailure != null) onFailure.run();
            }
        }));
    }

    public void removeVf(final @NotNull Project p, final Object requester, final Path path) {
        try {
            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(path.toFile());
            if (vf != null) {
                WriteAction.run(() -> vf.delete(requester));
            }
        } catch (final IOException ex) {
            Services.getInstance(p, Notifier.class).error(p, "Could not delete file: " + ex.getMessage(), "Error");
        }
    }

}
