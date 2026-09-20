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
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.FileKind;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
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
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@AllArgsConstructor
final class RunWriter {
    private final @NotNull Project p;
    private final @NotNull IndexerDataStore store;

    private final @NotNull ExecutorService queue =
            AppExecutorUtil.createBoundedApplicationPoolExecutor("Testin Run Status Writer", 1);

    private final @NotNull Map<Path, byte[]> unwritten = new ConcurrentHashMap<>();

    void create(final @NotNull Path runPath, final @NotNull TestRunDto tr) {
        store.registerTestRun(runPath, tr);
        write(runPath, tr, Set.of());
    }

    void persist(final @NotNull Path runPath, final @NotNull TestRunDto tr, final @NotNull Set<UUID> gone) {
        if (store.findTestRun(runPath).isEmpty()) {
            Logger.info("Test run no longer indexed, so it was not written back: " + runPath.getFileName());
            return;
        }

        store.registerTestRun(runPath, tr);
        write(runPath, tr, gone);
    }

    // Rule-INTERNAL-011
    private void write(final @NotNull Path runPath, final @NotNull TestRunDto tr, final @NotNull Set<UUID> gone) {
        final @NotNull Set<String> named = namedScreenshots(tr);

        final @NotNull Map<Path, byte[]> results = new LinkedHashMap<>();
        for (final TestRunItems item : tr.getResults()) {
            snapshot(item, "run item").ifPresent(bytes -> results.put(runPath.resolve(FileKind.RUN_ITEM.fileName(item.getId())), bytes));
        }

        queue.execute(() -> {
            try {
                if (store.findTestRun(runPath).isEmpty()) {
                    Logger.info("Test run removed before its results were written, so nothing was written: " + runPath.getFileName());
                    return;
                }

                final @NotNull TestDataFiles files = Services.getInstance(p, TestDataFiles.class);
                results.forEach((file, bytes) -> {
                    if (files.alreadyHolds(file, bytes)) return;

                    files.write(p, file, bytes);
                    Logger.trace("Result written for " + runPath.getFileName() + ": " + file.getFileName());
                });

                removeResultsOf(files, runPath, gone);
                sweepScreenshots(files, runPath, named);
            } catch (final Exception ex) {
                Logger.error("Failed to persist test run data: " + ex.getMessage());
            }
        });
    }

    // UC-TREE-PANEL-022, Rule-INTERNAL-011
    private void removeResultsOf(final @NotNull TestDataFiles files, final @NotNull Path runPath, final @NotNull Set<UUID> gone) {
        for (final UUID id : gone) {
            final @NotNull Path file = runPath.resolve(FileKind.RUN_ITEM.fileName(id));
            if (!Files.exists(file)) continue;

            Logger.info("Removing the result of a case the run no longer covers: " + file.getFileName());
            files.delete(p, file);
        }
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    private void sweepScreenshots(final @NotNull TestDataFiles files, final @NotNull Path runPath, final @NotNull Set<String> named) {
        files.screenshotsIn(runPath).stream()
                .filter(file -> !named.contains(file.getFileName().toString()))
                .forEach(file -> files.delete(p, file));
    }

    private static @NotNull Set<String> namedScreenshots(final @NotNull TestRunDto tr) {
        return tr.getResults().stream()
                .flatMap(item -> item.getScreenshots().stream())
                .collect(Collectors.toSet());
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    @NotNull List<String> storeScreenshots(final @NotNull Path runPath, final @NotNull List<byte[]> pngs) {
        final @NotNull Set<String> taken = new HashSet<>(store.findTestRun(runPath).map(RunWriter::namedScreenshots).orElse(Set.of()));
        final @NotNull Map<String, byte[]> byName = new LinkedHashMap<>();
        for (final byte[] png : pngs) {
            final @NotNull String name = TestRunDirectoryDto.newScreenshotName(taken);
            taken.add(name);
            byName.put(name, png);
        }

        byName.forEach((name, png) -> unwritten.put(TestRunDirectoryDto.screenshotFile(runPath, name), png));

        if (!byName.isEmpty()) queue.execute(() -> {
            try {
                if (store.findTestRun(runPath).isEmpty()) {
                    Logger.info("Test run removed before its screenshots were written, so nothing was written: " + runPath.getFileName());
                    return;
                }

                final @NotNull TestDataFiles files = Services.getInstance(p, TestDataFiles.class);
                byName.forEach((name, png) -> files.write(p, TestRunDirectoryDto.screenshotFile(runPath, name), png));
            } catch (final Exception ex) {
                Logger.error("Failed to write the screenshots of " + runPath.getFileName() + ": " + ex.getMessage());
            } finally {
                byName.forEach((name, png) -> unwritten.remove(TestRunDirectoryDto.screenshotFile(runPath, name), png));
            }
        });

        return List.copyOf(byName.keySet());
    }

    byte @NotNull [] readScreenshot(final @NotNull Path runPath, final @NotNull String name) {
        if (!TestRunDirectoryDto.isScreenshotName(name)) return new byte[0];

        final @NotNull Path file = TestRunDirectoryDto.screenshotFile(runPath, name);
        return Optional.ofNullable(unwritten.get(file)).orElseGet(() -> Services.getInstance(p, TestDataFiles.class).readBytes(file));
    }

    // Rule-INTERNAL-090
    void persistMarker(final @NotNull Path runPath) {
        queue.execute(() -> {
            try {
                if (store.findTestRun(runPath).isEmpty()) {
                    Logger.info("Test run removed before its marker was written, so nothing was written: " + runPath.getFileName());
                    return;
                }

                if (store.persistRunMarker(runPath)) Logger.trace("Marker persisted for " + runPath.getFileName());
            } catch (final Exception ex) {
                Logger.error("Failed to persist marker: " + ex.getMessage());
            }
        });
    }

    private @NotNull Optional<byte[]> snapshot(final @NotNull Object value, final @NotNull String what) {
        try {
            return Optional.of(Services.getInstance(p, Mapper.class).writeValueAsBytes(value));
        } catch (final Exception ex) {
            Logger.error("Failed to snapshot " + what + ": " + ex.getMessage());
            return Optional.empty();
        }
    }
}
