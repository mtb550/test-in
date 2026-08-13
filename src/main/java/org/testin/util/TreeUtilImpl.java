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
import org.jetbrains.annotations.Nullable;
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
        // The lookup runs off the EDT and the operation on it: refreshAndFindFile
        // refreshes synchronously and reads the VFS persistence, which the EDT is
        // not allowed to do, while the operation itself mutates the VFS and so
        // needs the write action.
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @Nullable VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (vf == null) {
                    Services.getInstance(p, Notifier.class).error(p, "Could not find path on disk:\n" + path, errorTitle);
                    return;
                }

                WriteAction.run(() -> {
                    try {
                        operation.execute(vf);
                    } catch (final Exception ex) {
                        // A failed VFS operation is reported, never thrown into the
                        // EDT as an exception dialog (parity with the two-path form).
                        Services.getInstance(p, Notifier.class).error(p, "Operation failed: " + ex.getMessage(), errorTitle);
                    }
                });
            });
        });
    }

    public void executeVfsAction(final @NotNull Project p, final @NotNull Path sourcePath, final @NotNull Path targetPath, final @NotNull String errorTitle, final @NotNull IVfsBiOperation operation) {
        executeVfsAction(p, sourcePath, targetPath, errorTitle, operation, null);
    }

    public void executeVfsAction(final @NotNull Project p, final @NotNull Path sourcePath, final @NotNull Path targetPath,
                                 final @NotNull String errorTitle, final @NotNull IVfsBiOperation operation,
                                 final @Nullable Runnable onSuccess) {
        executeVfsAction(p, sourcePath, targetPath, errorTitle, operation, onSuccess, null);
    }

    public void executeVfsAction(final @NotNull Project p, final @NotNull Path sourcePath, final @NotNull Path targetPath,
                                 final @NotNull String errorTitle, final @NotNull IVfsBiOperation operation,
                                 final @Nullable Runnable onSuccess, final @Nullable Runnable onFailure) {
        // Both lookups off the EDT, the operation on it - see the single-path form.
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @Nullable VirtualFile sourceVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(sourcePath);
            final @Nullable VirtualFile targetVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(targetPath);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (sourceVf == null || targetVf == null) {
                    Services.getInstance(p, Notifier.class).error(p, "Could not find source or target path on disk.", errorTitle);
                    if (onFailure != null) onFailure.run();
                    return;
                }

                WriteAction.run(() -> {
                    try {
                        operation.execute(sourceVf, targetVf);
                        if (onSuccess != null) onSuccess.run();
                    } catch (final Exception ex) {
                        Services.getInstance(p, Notifier.class).error(p, "Operation failed: " + ex.getMessage(), errorTitle);
                        if (onFailure != null) onFailure.run();
                    }
                });
            });
        });
    }

    /**
     * Deletes the file, then runs {@code onDeleted} on the EDT.
     * <p>
     * The callback exists because the lookup has to leave the EDT, which makes
     * the delete asynchronous; callers that update the indexer cache afterwards
     * must wait for it. It runs whether or not the file was there: a path that
     * has already gone still has to be cleared from the cache, which is what the
     * synchronous version did by simply returning.
     */
    public void removeVf(final @NotNull Project p, final @NotNull Object requester, final @NotNull Path path,
                         final @NotNull Runnable onDeleted) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @Nullable VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);

            ApplicationManager.getApplication().invokeLater(() -> {
                WriteAction.run(() -> {
                    try {
                        if (vf != null) vf.delete(requester);
                    } catch (final IOException ex) {
                        Services.getInstance(p, Notifier.class).error(p, "Could not delete file: " + ex.getMessage(), "Error");
                    }
                });

                onDeleted.run();
            });
        });
    }

}
