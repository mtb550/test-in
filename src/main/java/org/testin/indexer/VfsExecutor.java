package org.testin.indexer;

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
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.VfsBiOperation;
import org.testin.util.VfsOperation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Performs VFS mutations. Package-private, and in this package, so that the
 * architecture rule is enforced by the compiler rather than by convention: the
 * indexer is the single owner of test data file access, and nothing outside it
 * can reach this executor at all.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
final class VfsExecutor {

    // Renaming is the only single-path VFS operation, so the title it reports a
    // failure under is not a parameter: one caller, one word, and a second
    // operation would bring its own method rather than a second string.
    void executeVfsAction(final @NotNull Project p, final @NotNull Path path, final @NotNull VfsOperation operation) {
        final String errorTitle = "Rename Failed";

        // The lookup runs off the EDT and the operation on it: refreshAndFindFile
        // refreshes synchronously and reads the VFS persistence, which the EDT is
        // not allowed to do, while the operation itself mutates the VFS and so
        // needs the write action.
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // Boundary: the VFS answers null for a path it cannot resolve -
            // deleted underneath us, or never refreshed into it (#71).
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

    void executeVfsAction(final @NotNull Project p, final @NotNull Path sourcePath, final @NotNull Path targetPath,
                          final @NotNull String errorTitle, final @NotNull VfsBiOperation operation,
                          final @NotNull Runnable onSuccess, final @NotNull Runnable onFailure) {
        // Both lookups off the EDT, the operation on it - see the single-path form.
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // Boundary: the VFS answers null for a path it cannot resolve (#71).
            final @Nullable VirtualFile sourceVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(sourcePath);
            final @Nullable VirtualFile targetVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(targetPath);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (sourceVf == null || targetVf == null) {
                    Services.getInstance(p, Notifier.class).error(p, "Could not find source or target path on disk.", errorTitle);
                    onFailure.run();
                    return;
                }

                WriteAction.run(() -> {
                    try {
                        operation.execute(sourceVf, targetVf);
                        onSuccess.run();
                    } catch (final Exception ex) {
                        Services.getInstance(p, Notifier.class).error(p, "Operation failed: " + ex.getMessage(), errorTitle);
                        onFailure.run();
                    }
                });
            });
        });
    }

    /**
     * Deletes the file, then reports on the EDT whether it is gone.
     * <p>
     * The callback exists because the lookup has to leave the EDT, which makes
     * the deletion asynchronous; callers that update the indexer cache afterward
     * must wait for it.
     * <p>
     * It reports whether the file is gone, not merely that the attempt finished.
     * The callback used to run either way, so a caller could not tell a deletion
     * that failed from one that worked — and the indexer's cache update ran on
     * both, dropping a node that was still on disk (#66, F2).
     * <p>
     * A path the VFS cannot find counts as deleted: there is nothing left to
     * remove, and the cache should stop describing it.
     */
    void removeVf(final @NotNull Project p, final @NotNull Object requester, final @NotNull Path path,
                  final @NotNull Consumer<@NotNull Boolean> onDeleted) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // Boundary: the VFS answers null for a path it cannot resolve -
            // deleted underneath us, or never refreshed into it (#71).
            final @Nullable VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);

            ApplicationManager.getApplication().invokeLater(() -> {
                final AtomicBoolean deleted = new AtomicBoolean(true);

                WriteAction.run(() -> {
                    try {
                        if (vf != null) vf.delete(requester);
                    } catch (final IOException ex) {
                        deleted.set(false);
                        Services.getInstance(p, Notifier.class).error(p, "Could not delete file: " + ex.getMessage(), "Error");
                    }
                });

                onDeleted.accept(deleted.get());
            });
        });
    }

}
