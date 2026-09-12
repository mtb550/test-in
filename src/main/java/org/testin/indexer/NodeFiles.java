/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.indexer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.DirectoryType;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Mapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

/**
 * Where a node's files are: deleting them, moving them, copying them, renaming
 * them - and putting the cache right afterwards, in that order.
 * <p>
 * <b>The ordering rule this class exists to keep.</b> The cache update runs only
 * <b>after</b> the VFS operation succeeded, never before, and only when it
 * succeeded. A marker write creates directories, so a cache update that ran
 * first left a directory the rename then failed on ("already exists in VFS"),
 * and one that ran either way dropped a node the VFS had refused to delete -
 * the tree stopped showing something that was still on disk (#66, F2).
 * <p>
 * <b>Every operation reports what happened</b>, not merely that it is over. The
 * callbacks used to be bare Runnables that fired on both outcomes, so a caller
 * had to read the cache back to find out whether the move it asked for
 * happened.
 * <p>
 * Package-private, and reached only through {@link ProjectIndexer}, which is the
 * single door to file access. It hands itself in as the VFS requestor, so the
 * platform still sees these operations coming from the indexer, and as the way
 * back to the rescan a copy needs.
 */
@AllArgsConstructor
final class NodeFiles {

    private final @NotNull Project p;
    private final @NotNull ProjectIndexer indexer;
    private final @NotNull IndexerDataStore store;

    /**
     * Deletes on disk, refreshes, and only then updates the cache - the order
     * CLAUDE.md requires. The refresh is asynchronous: the synchronous one ran
     * on the EDT, and a full VFS refresh there is a slow operation.
     */
    void remove(final @NotNull Path path, final @NotNull Runnable cacheUpdate, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        Services.getInstance(p, VfsExecutor.class).removeVf(p, indexer, path,
                deleted -> VirtualFileManager.getInstance().asyncRefresh(() -> {
                    if (deleted) cacheUpdate.run();
                    onRemoved.accept(deleted);
                }));
    }

    /**
     * Moves the node, and reports whether it moved.
     */
    void move(final @NotNull Path oldPath, final @NotNull Path newPath, final @NotNull Consumer<@NotNull Boolean> onFinished) {
        final @NotNull Optional<Path> found = Optional.ofNullable(newPath.getParent());
        if (found.isEmpty()) {
            Logger.warn("Move refused, target has no parent directory: " + newPath);
            onFinished.accept(false);
            return;
        }

        final @NotNull Path targetParent = found.orElseThrow();

        Services.getInstance(p, VfsExecutor.class).executeVfsAction(p, oldPath, targetParent, Bundle.message("vfs.move.failed.title"), (sourceVf, targetVf) -> {
            try {
                sourceVf.move(indexer, targetVf);
            } catch (final IOException ex) {
                Logger.error(ex.getMessage());
                throw new RuntimeException(ex);
            }
        }, () -> {
            store.renameNode(oldPath, newPath);
            Logger.info("Moved successfully to: " + newPath);
            onFinished.accept(true);
        }, () -> onFinished.accept(false));
    }

    /**
     * Copies each source into the target, and reports how many arrived - not how
     * many were attempted. Every copy runs its own VFS action and any of them
     * can fail on its own, so the count is the only honest answer.
     */
    void copy(final @NotNull List<Path> sourcePaths, final @NotNull Path targetPath, final @NotNull IntConsumer onComplete) {
        if (sourcePaths.isEmpty()) {
            onComplete.accept(0);
            return;
        }

        final @NotNull AtomicInteger pending = new AtomicInteger(sourcePaths.size());
        final @NotNull AtomicInteger copied = new AtomicInteger();

        // The subtrees that actually arrived, waiting to be given fresh ids.
        // Collected rather than rewritten in place, because the rewrite is a
        // directory walk plus a read, a write and a delete for every case in
        // the copy - and the callback that used to do it runs on the UI thread
        // inside the write action the copy holds. A test set of any size froze
        // the whole IDE for as long as it took, with no progress bar and no way
        // to cancel, while the much cheaper re-index beside it had already been
        // moved off the UI thread.
        final @NotNull List<Path> arrived = new CopyOnWriteArrayList<>();

        // Both outcomes drain the counter, so the tree is still rebuilt when a
        // copy fails; only the success path raises the count.
        final @NotNull Runnable operationFinished = () -> {
            if (pending.decrementAndGet() != 0) return;
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                // Before the re-index, which is the ordering that matters: the
                // scanner takes a case's identity from its file name, so the
                // new ids have to be on disk before the index reads them.
                arrived.forEach(this::reidentifyCopiedCases);

                indexer.refreshIndexedProject(targetPath);
                ApplicationManager.getApplication().invokeLater(() -> onComplete.accept(copied.get()));
            });
        };
        final @NotNull Runnable operationSucceeded = () -> {
            copied.incrementAndGet();
            operationFinished.run();
        };

        for (final Path sourcePath : sourcePaths) {
            final @NotNull Path copiedRoot = targetPath.resolve(sourcePath.getFileName());

            final @NotNull Runnable copySucceeded = () -> {
                arrived.add(copiedRoot);
                operationSucceeded.run();
            };

            Services.getInstance(p, VfsExecutor.class).executeVfsAction(p, sourcePath, targetPath, Bundle.message("vfs.copy.failed.title"), (sourceVf, targetVf) -> {
                try {
                    sourceVf.copy(indexer, targetVf, sourceVf.getName());
                } catch (final IOException ex) {
                    Logger.error(ex.getMessage());
                    throw new RuntimeException(ex);
                }
            }, copySucceeded, operationFinished);
        }
    }

    /**
     * Renames the node. Unlike the copy and move forms this needs no success
     * flag: the whole body is one VFS operation, and {@code executeVfsAction}
     * reports and swallows a failure before the cache update and the callback
     * are reached.
     * <p>
     * Nothing here knows what kind of node it is renaming, and nothing needs to.
     * A test run briefly did - its results were named after the folder, so the
     * rename had to carry them - and that special case went away when the name
     * stopped depending on the folder (#177).
     */
    void rename(final @NotNull Path oldPath, final @NotNull Path newPath, final @NotNull Runnable onFinished) {
        Services.getInstance(p, VfsExecutor.class).executeVfsAction(p, oldPath, vf -> {
            try {
                vf.rename(indexer, newPath.getFileName().toString());
            } catch (final IOException ex) {
                Logger.error(ex.getMessage());
                throw new RuntimeException(ex);
            }

            // The cache update persists the touched marker at the NEW path, and
            // that write creates directories. So it must run only after the VFS
            // rename succeeded: otherwise the target directory already exists
            // and the rename fails with "already exists in VFS".
            store.renameNode(oldPath, newPath);
            onFinished.run();
        });
    }

    /**
     * UC-TREE-PANEL-014, Rule-TREE-PANEL-051.
     * <p>
     * Gives every test case in a freshly copied subtree an id of its own.
     * <p>
     * A copy is a copy of the files, so the cases in it arrive carrying the ids
     * of the cases they came from - and a case's id is its identity here: the
     * index holds one case per id, so the copy and the original would resolve to
     * the same case, and editing either would edit both. Pasting a single case
     * has always taken a fresh id; copying a whole set never went through that
     * code (#51).
     * <p>
     * Before the index reads them, and by the file name, because the file name
     * is what the scanner takes the identity from - the id inside is rewritten
     * to match so the two never disagree.
     * <p>
     * Test runs are left alone. Their file is named for their folder rather than
     * for an id, so they are not touched by this, and a copied run still refers
     * to the cases it actually executed.
     * <p>
     * A case whose file a tester named by hand comes through here like any
     * other, and leaves with the name Testin gives - a fresh id, and the file
     * called after it. The name it had was the tester's on the original, which
     * keeps it; the copy is a case Testin wrote.
     */
    private void reidentifyCopiedCases(final @NotNull Path copiedRoot) {
        final List<Path> caseFiles;

        try (Stream<Path> files = Files.walk(copiedRoot)) {
            // Collected before rewriting: the walk is lazy, and creating and
            // deleting files under it while it runs is not its contract.
            caseFiles = files.filter(Files::isRegularFile)
                    .filter(file -> ProjectIndexer.isCaseFile(file, dir -> store.hasMarker(dir, DirectoryType.TS)))
                    .toList();

        } catch (final IOException ex) {
            Logger.error("Could not read the copied nodes at " + copiedRoot + ": " + ex.getMessage());
            return;
        }

        caseFiles.forEach(this::reidentify);
        Logger.info("Gave " + caseFiles.size() + " copied test case(s) new ids under " + copiedRoot.getFileName());
    }

    private void reidentify(final @NotNull Path caseFile) {
        try {
            final @NotNull TestCaseDto tc = Services.getInstance(p, Mapper.class).readValue(caseFile.toFile(), TestCaseDto.class);
            final @NotNull UUID fresh = UUID.randomUUID();

            tc.setId(fresh);
            Services.getInstance(p, TestDataFiles.class).write(p, caseFile.resolveSibling(fresh + ".json"), tc);
            Files.delete(caseFile);

        } catch (final Exception ex) {
            Logger.error("Could not give the copied case " + caseFile.getFileName() + " a new id: " + ex.getMessage());
        }
    }
}
