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

import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.testin.logger.Logger;
import org.testin.model.FileKind;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.util.FailureText;
import org.testin.util.Mapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

final class TestRunWriter {
    private final @NotNull IndexerDataStore store;
    private final @NotNull TestDataFiles files;
    private final @NotNull Mapper mapper;

    private final @NotNull ExecutorService queue =
            AppExecutorUtil.createBoundedApplicationPoolExecutor("Testin Test Run Writer", 1);

    private final @NotNull Map<Path, byte[]> unwritten = new ConcurrentHashMap<>();

    TestRunWriter(final @NotNull Project p, final @NotNull IndexerDataStore store) {
        this.store = store;
        this.files = Services.getInstance(p, TestDataFiles.class);
        this.mapper = Services.getInstance(p, Mapper.class);
    }

    private static @NotNull Set<String> namedScreenshots(final @NotNull TestRunDto tr) {
        return tr.getResults().stream()
                .flatMap(item -> item.getScreenshots().stream())
                .collect(Collectors.toSet());
    }

    void create(final @NotNull Path testRunPath, final @NotNull TestRunDto tr) {
        store.registerTestRun(testRunPath, tr);
        write(testRunPath, tr, tr.getResults(), Set.of());
    }

    void persist(final @NotNull Path testRunPath, final @NotNull TestRunDto tr, final @NotNull Set<UUID> gone) {
        if (store.findTestRun(testRunPath).isEmpty()) {
            Logger.info("Test run no longer indexed, so it was not written back: " + testRunPath.getFileName());
            return;
        }

        store.registerTestRun(testRunPath, tr);
        write(testRunPath, tr, tr.getResults(), gone);
    }

    // Rule-INTERNAL-011
    void persistResult(final @NotNull Path testRunPath, final @NotNull TestRunDto tr, final @NotNull TestRunItems result) {
        write(testRunPath, tr, List.of(result), Set.of());
    }

    // Rule-INTERNAL-011
    private void write(final @NotNull Path testRunPath, final @NotNull TestRunDto tr, final @NotNull List<TestRunItems> changed, final @NotNull Set<UUID> gone) {
        final @NotNull Set<String> named = namedScreenshots(tr);

        final @NotNull Map<Path, byte[]> results = new LinkedHashMap<>();
        for (final TestRunItems item : changed) {
            snapshot(item).ifPresent(bytes -> results.put(testRunPath.resolve(FileKind.RUN_ITEM.fileName(item.getId())), bytes));
        }

        queue.execute(() -> {
            try {
                if (store.findTestRun(testRunPath).isEmpty()) {
                    Logger.info("Test run removed before its results were written, so nothing was written: " + testRunPath.getFileName());
                    return;
                }

                results.forEach((file, bytes) -> {
                    if (files.alreadyHolds(file, bytes)) return;

                    files.write(file, bytes);
                    Logger.trace("Result written for " + testRunPath.getFileName() + ": " + file.getFileName());
                });

                removeResultsOf(testRunPath, gone);
                sweepScreenshots(testRunPath, named);
            } catch (final Exception ex) {
                Logger.error("Failed to persist test run data: " + FailureText.of(ex));
            }
        });
    }

    // UC-TREE-PANEL-022, Rule-INTERNAL-011
    private void removeResultsOf(final @NotNull Path testRunPath, final @NotNull Set<UUID> gone) {
        for (final UUID id : gone) {
            final @NotNull Path file = testRunPath.resolve(FileKind.RUN_ITEM.fileName(id));
            if (!Files.exists(file)) continue;

            Logger.info("Removing the result of a test case the test run no longer covers: " + file.getFileName());
            files.delete(file);
        }
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    private void sweepScreenshots(final @NotNull Path testRunPath, final @NotNull Set<String> named) {
        files.screenshotsIn(testRunPath).stream()
                .filter(file -> !named.contains(file.getFileName().toString()))
                .forEach(files::delete);
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    @NotNull List<String> storeScreenshots(final @NotNull Path testRunPath, final @NotNull List<byte[]> pngs) {
        final @NotNull Set<String> taken = new HashSet<>(store.findTestRun(testRunPath).map(TestRunWriter::namedScreenshots).orElse(Set.of()));
        final @NotNull Map<String, byte[]> byName = new LinkedHashMap<>();
        for (final byte[] png : pngs) {
            final @NotNull String name = TestRunDirectoryDto.newScreenshotName(taken);
            taken.add(name);
            byName.put(name, png);
        }

        byName.forEach((name, png) -> unwritten.put(TestRunDirectoryDto.screenshotFile(testRunPath, name), png));

        if (!byName.isEmpty()) queue.execute(() -> {
            try {
                if (store.findTestRun(testRunPath).isEmpty()) {
                    Logger.info("Test run removed before its screenshots were written, so nothing was written: " + testRunPath.getFileName());
                    return;
                }

                byName.forEach((name, png) -> files.write(TestRunDirectoryDto.screenshotFile(testRunPath, name), png));
            } catch (final Exception ex) {
                Logger.error("Failed to write the screenshots of " + testRunPath.getFileName() + ": " + FailureText.of(ex));
            } finally {
                byName.forEach((name, png) -> unwritten.remove(TestRunDirectoryDto.screenshotFile(testRunPath, name), png));
            }
        });

        return List.copyOf(byName.keySet());
    }

    byte @NotNull [] readScreenshot(final @NotNull Path testRunPath, final @NotNull String name) {
        if (!TestRunDirectoryDto.isScreenshotName(name)) return new byte[0];

        final @NotNull Path file = TestRunDirectoryDto.screenshotFile(testRunPath, name);
        return Optional.ofNullable(unwritten.get(file)).orElseGet(() -> files.readBytes(file));
    }

    // Rule-INTERNAL-090
    void persistMarker(final @NotNull Path testRunPath) {
        queue.execute(() -> {
            try {
                if (store.findTestRun(testRunPath).isEmpty()) {
                    Logger.info("Test run removed before its marker was written, so nothing was written: " + testRunPath.getFileName());
                    return;
                }

                if (store.persistTestRunMarker(testRunPath)) Logger.trace("Marker persisted for " + testRunPath.getFileName());
            } catch (final Exception ex) {
                Logger.error("Failed to persist marker: " + FailureText.of(ex));
            }
        });
    }

    @TestOnly
    void awaitWrites() {
        try {
            queue.submit(() -> {
            }).get(10, TimeUnit.SECONDS);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while the test run writer was writing", ex);
        } catch (final ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("The test run writer did not finish: " + FailureText.of(ex), ex);
        }
    }

    private @NotNull Optional<byte[]> snapshot(final @NotNull TestRunItems item) {
        try {
            return Optional.of(mapper.writeValueAsBytes(item));
        } catch (final Exception ex) {
            Logger.error("Failed to snapshot run item: " + FailureText.of(ex));
            return Optional.empty();
        }
    }
}
