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
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.VfsBiOperation;
import org.testin.util.VfsOperation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
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

    /**
     * The file at this path, and empty when the VFS cannot resolve it - deleted
     * underneath us, or never refreshed into it.
     * <p>
     * Off the EDT by contract: refreshAndFindFile refreshes synchronously and
     * reads the VFS persistence, which the EDT is not allowed to do. The one
     * place the platform's null becomes an answer of its own, so the three
     * operations below are written as though a path always resolves (#71).
     */
    private static @NotNull Optional<VirtualFile> find(final @NotNull Path path) {
        return Optional.ofNullable(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path));
    }

    /**
     * Says this change is the plugin's own, so the file watcher does not read
     * the project again for something the plugin is already redrawing (#20).
     * <p>
     * Claimed before the operation runs, because the VFS event can arrive while
     * it is still running.
     */
    private static void claim(final @NotNull Path path) {
        Services.getInstance(OwnWrites.class).record(path);
    }

    // Renaming is the only single-path VFS operation, so the title it reports a
    // failure under is not a parameter: one caller, one word, and a second
    // operation would bring its own method rather than a second string.
    void executeVfsAction(final @NotNull Project p, final @NotNull Path path, final @NotNull VfsOperation operation) {
        final @NotNull String errorTitle = "Rename Failed";
        claim(path);

        // The lookup runs off the EDT and the operation on it: refreshAndFindFile
        // refreshes synchronously and reads the VFS persistence, which the EDT is
        // not allowed to do, while the operation itself mutates the VFS and so
        // needs the write action.
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull Optional<VirtualFile> vf = find(path);

            ApplicationManager.getApplication().invokeLater(() -> vf.ifPresentOrElse(
                    file -> WriteAction.run(() -> {
                        try {
                            operation.execute(file);
                        } catch (final Exception ex) {
                            // A failed VFS operation is reported, never thrown into the
                            // EDT as an exception dialog (parity with the two-path form).
                            Services.getInstance(p, Notifier.class).error(p, errorTitle, "Operation failed: " + ex.getMessage());
                        }
                    }),
                    () -> Services.getInstance(p, Notifier.class)
                            .error(p, errorTitle, "Could not find path on disk:\n" + path)));
        });
    }

    void executeVfsAction(final @NotNull Project p, final @NotNull Path sourcePath, final @NotNull Path targetPath, final @NotNull String errorTitle, final @NotNull VfsBiOperation operation, final @NotNull Runnable onSuccess, final @NotNull Runnable onFailure) {
        claim(sourcePath);
        claim(targetPath);

        // Both lookups off the EDT, the operation on it - see the single-path form.
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull Optional<VirtualFile> sourceVf = find(sourcePath);
            final @NotNull Optional<VirtualFile> targetVf = find(targetPath);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (sourceVf.isEmpty() || targetVf.isEmpty()) {
                    Services.getInstance(p, Notifier.class).error(p, errorTitle, "Could not find source or target path on disk.");
                    onFailure.run();
                    return;
                }

                WriteAction.run(() -> {
                    try {
                        operation.execute(sourceVf.get(), targetVf.get());
                        onSuccess.run();
                    } catch (final Exception ex) {
                        Services.getInstance(p, Notifier.class).error(p, errorTitle, "Operation failed: " + ex.getMessage());
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
    void removeVf(final @NotNull Project p, final @NotNull Object requester, final @NotNull Path path, final @NotNull Consumer<@NotNull Boolean> onDeleted) {
        claim(path);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            // The recycle bin first, and on this thread rather than in the write
            // action below: trashing a whole test project is the platform
            // walking it file by file, which is not work for the EDT. A desktop
            // with no bin answers no and the VFS delete runs as it always did.
            if (Trash.accepted(path)) {
                ApplicationManager.getApplication().invokeLater(() -> onDeleted.accept(true));
                return;
            }

            final @NotNull Optional<VirtualFile> vf = find(path);

            ApplicationManager.getApplication().invokeLater(() -> {
                final @NotNull AtomicBoolean deleted = new AtomicBoolean(true);

                WriteAction.run(() -> {
                    try {
                        // A path the VFS cannot find counts as deleted - see above.
                        if (vf.isPresent()) vf.get().delete(requester);
                    } catch (final IOException ex) {
                        deleted.set(false);
                        Services.getInstance(p, Notifier.class).error(p, "Delete Failed", "Could not delete file: " + ex.getMessage());
                    }
                });

                onDeleted.accept(deleted.get());
            });
        });
    }

}
