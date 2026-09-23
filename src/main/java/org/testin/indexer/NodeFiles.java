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
import org.testin.model.FileKind;
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

@AllArgsConstructor
final class NodeFiles {
    private final @NotNull Project p;
    private final @NotNull ProjectIndexer indexer;
    private final @NotNull IndexerDataStore store;

    void remove(final @NotNull Path path, final @NotNull Runnable cacheUpdate, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        Services.getInstance(p, VfsExecutor.class).removeVf(p, indexer, path,
                deleted -> VirtualFileManager.getInstance().asyncRefresh(() -> {
                    if (deleted) cacheUpdate.run();
                    onRemoved.accept(deleted);
                }));
    }

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
        }, () -> followOnDisk(oldPath, newPath, () -> {
            Logger.info("Moved successfully to: " + newPath);
            onFinished.accept(true);
        }), () -> onFinished.accept(false));
    }

    private void followOnDisk(final @NotNull Path oldPath, final @NotNull Path newPath, final @NotNull Runnable then) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            store.renameNode(oldPath, newPath);
            ApplicationManager.getApplication().invokeLater(then);
        });
    }

    void copy(final @NotNull List<Path> sourcePaths, final @NotNull Path targetPath, final @NotNull IntConsumer onComplete) {
        if (sourcePaths.isEmpty()) {
            onComplete.accept(0);
            return;
        }

        final @NotNull AtomicInteger pending = new AtomicInteger(sourcePaths.size());
        final @NotNull AtomicInteger copied = new AtomicInteger();

        final @NotNull List<Path> arrived = new CopyOnWriteArrayList<>();

        final @NotNull Runnable operationFinished = () -> {
            if (pending.decrementAndGet() != 0) return;
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                arrived.forEach(this::reidentifyCopiedTestCases);

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

    void rename(final @NotNull Path oldPath, final @NotNull Path newPath, final @NotNull Runnable onFinished) {
        Services.getInstance(p, VfsExecutor.class).executeVfsAction(p, oldPath, vf -> {
            try {
                vf.rename(indexer, newPath.getFileName().toString());
            } catch (final IOException ex) {
                Logger.error(ex.getMessage());
                throw new RuntimeException(ex);
            }

            followOnDisk(oldPath, newPath, onFinished);
        });
    }

    // UC-TREE-PANEL-014, Rule-TREE-PANEL-051
    private void reidentifyCopiedTestCases(final @NotNull Path copiedRoot) {
        final List<Path> testCaseFiles;
        final List<Path> markerFiles;

        try (Stream<Path> files = Files.walk(copiedRoot)) {
            final @NotNull List<Path> all = files.filter(Files::isRegularFile).toList();

            testCaseFiles = all.stream()
                    .filter(file -> ProjectIndexer.isTestCaseFile(file, dir -> store.hasMarker(dir, DirectoryType.TS)))
                    .toList();

            markerFiles = all.stream()
                    .filter(file -> DirectoryType.byMarker(String.valueOf(file.getFileName())).isPresent())
                    .toList();

        } catch (final IOException ex) {
            Logger.error("Could not read the copied nodes at " + copiedRoot + ": " + ex.getMessage());
            return;
        }

        final long given = testCaseFiles.stream().filter(this::reidentify).count();
        Logger.info("Gave " + given + " of " + testCaseFiles.size() + " copied test case(s) new ids under " + copiedRoot.getFileName());

        // Rule-INTERNAL-090
        final long folders = markerFiles.stream().filter(store::giveFreshMarkerId).count();
        Logger.info("Gave " + folders + " of " + markerFiles.size() + " copied folder(s) ids of their own under " + copiedRoot.getFileName());
    }

    // UC-TREE-PANEL-014, Rule-TREE-PANEL-051
    private boolean reidentify(final @NotNull Path testCaseFile) {
        try {
            final @NotNull TestCaseDto tc = Services.getInstance(p, Mapper.class).readValue(testCaseFile.toFile(), TestCaseDto.class);
            final @NotNull UUID fresh = UUID.randomUUID();

            tc.setId(fresh);
            if (!Services.getInstance(p, TestDataFiles.class).write(p, testCaseFile.resolveSibling(FileKind.TEST_CASE.fileName(fresh)), tc))
                return false;

            Services.getInstance(OwnWrites.class).record(p, testCaseFile);
            Files.delete(testCaseFile);
            return true;

        } catch (final Exception ex) {
            Logger.error("Could not give the copied case " + testCaseFile.getFileName() + " a new id: " + ex.getMessage());
            return false;
        }
    }
}
