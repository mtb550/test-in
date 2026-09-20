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

/**
 * The one writer of a test run's files: its results, its marker and the
 * screenshots its failures name.
 * <p>
 * <b>Snapshot here, write there.</b> The JSON is taken on the calling thread -
 * the EDT, for a verdict a tester just recorded - so it can never observe a
 * half-applied mutation, and only the disk I/O is handed to the queue.
 * <p>
 * <b>One queue, in submission order.</b> Every run write goes through a single
 * sequential executor, so two of them can never interleave. Saving Result
 * Analysis used to write straight from the UI thread while a verdict recorded
 * moments earlier was still queued, holding a snapshot taken before the
 * analysis existed; the queued write landed second and silently restored the
 * older file, and the tester found their analysis gone after the next reload.
 * <p>
 * Package-private, and reached only through {@link ProjectIndexer}: file access
 * has one door and this is behind it.
 */
/*
 * The queue is a final field with an initializer, so @AllArgsConstructor leaves
 * it out: the two collaborators are the arguments, and the writer is this
 * class's own.
 */
@AllArgsConstructor
final class RunWriter {

    private final @NotNull Project p;
    private final @NotNull IndexerDataStore store;

    /**
     * All run-status disk writes go through this one sequential executor,
     * so writes can never interleave or race each other.
     */
    private final @NotNull ExecutorService queue =
            AppExecutorUtil.createBoundedApplicationPoolExecutor("Testin Run Status Writer", 1);

    /**
     * Screenshots handed to the queue and not on disk yet, by file.
     * <p>
     * So a screenshot saved a moment ago is read back from here rather than by
     * waiting for the queue to empty. The wait ran on the EDT - the failure
     * form reads every screenshot of its row as it opens - so a tester who
     * opened a failure while the queue was busy got a frozen IDE for as long as
     * it took (#66, finding 174). A screenshot's name is never reused, so the bytes
     * held here are the bytes the file will hold.
     */
    private final @NotNull Map<Path, byte[]> unwritten = new ConcurrentHashMap<>();

    /**
     * A new run's results, and the index entry that says the run exists.
     * <p>
     * <b>The cache is written here, the file there.</b> The registration used to
     * sit inside the queued task, so the worker mutated the index on its own
     * thread, outside any order the caller could see - and a caller that created
     * a run and read it back on the next line was racing the queue for it.
     */
    void create(final @NotNull Path runPath, final @NotNull TestRunDto tr) {
        store.registerTestRun(runPath, tr);
        write(runPath, tr, Set.of());
    }

    /**
     * An existing run's results.
     * <p>
     * <b>A run that has gone is not written back.</b> Only {@link #create} puts a
     * run into the index. This used to register whatever it was handed first, so
     * a write for a run a sync or a delete had just taken away - a verdict, a
     * grid edit, an editor closing - put it back and wrote its file again (#66,
     * finding 143).
     */
    void persist(final @NotNull Path runPath, final @NotNull TestRunDto tr, final @NotNull Set<UUID> gone) {
        if (store.findTestRun(runPath).isEmpty()) {
            Logger.info("Test run no longer indexed, so it was not written back: " + runPath.getFileName());
            return;
        }

        store.registerTestRun(runPath, tr);
        write(runPath, tr, gone);
    }

    /**
     * Rule-INTERNAL-011.
     * <p>
     * One file per result, {@code <test case id>.ri}, and only the ones whose
     * bytes the run's folder does not already hold - so recording one verdict
     * writes one file where it used to rewrite every result of the run, and two
     * testers judging different cases of one cycle never touch the same file
     * (#305, G5).
     * <p>
     * Each result is serialized on the calling thread, with the screenshot names
     * the write will keep: the run the index holds is changed on the EDT, and the
     * queue writes later.
     * <p>
     * Between the submit and the execution the tester can delete the run, and
     * the queued task would then recreate its files with their parent
     * directories: a run folder on disk with no marker. So the write asks again
     * whether the run is still indexed, which is the one question that separates
     * a pending write from a resurrection (#66, finding 86).
     */
    private void write(final @NotNull Path runPath, final @NotNull TestRunDto tr, final @NotNull Set<UUID> gone) {
        // Taken with the snapshots, on the calling thread, so the sweep keeps
        // exactly the screenshots the written results name.
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

    /**
     * UC-TREE-PANEL-022, Rule-INTERNAL-011.
     * <p>
     * Removes the result files of the cases the change took out of the run - Edit
     * Test Run unticking one - with the screenshots they named, which the sweep
     * below then finds unnamed.
     * <p>
     * <b>Those, and nothing else in the folder.</b> A result file the run does
     * not cover is not evidence that anybody stopped covering it: it can be one a
     * pull brought a moment ago, or one this very write could not serialize, and
     * removing either loses a verdict nobody asked to lose. The caller says which
     * cases went, because the caller is the only one that saw the change (#305,
     * S21).
     */
    private void removeResultsOf(final @NotNull TestDataFiles files, final @NotNull Path runPath, final @NotNull Set<UUID> gone) {
        for (final UUID id : gone) {
            final @NotNull Path file = runPath.resolve(FileKind.RUN_ITEM.fileName(id));
            if (!Files.exists(file)) continue;

            Logger.info("Removing the result of a case the run no longer covers: " + file.getFileName());
            files.delete(p, file);
        }
    }

    /**
     * UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219.
     * <p>
     * Removes the screenshots in the run's folder that no run item names any
     * more, once the results that stopped naming them have landed (#313).
     * <p>
     * The one place a screenshot file goes. A pass, an automated failure, the x
     * on a thumbnail and Edit Test Run dropping a row only change names; this
     * removes what every one of them left behind, so none of them has to. After
     * the results, never before, and only when they landed: a screenshot the
     * file on disk still names is never removed.
     */
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

    /**
     * UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219.
     * <p>
     * Queues newly pasted screenshots as files beside the run, each under a new
     * name no result of the run holds, and answers the names in the order given
     * (#313).
     * <p>
     * Ahead of the results that name them, in the same queue, so a run file
     * never names a screenshot that has not landed. Written even over a file of
     * that name, which no result names and the sweep would take anyway. The run
     * is asked again at the write, as its results are, so a run removed
     * meanwhile does not get a folder back (#66, finding 86).
     */
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

    /**
     * A screenshot's PNG bytes, and none when its file is missing or the name
     * was never a screenshot's - a sync that brought the run before its
     * pictures, say. One still in the queue is answered from {@link #unwritten},
     * so it is read back rather than missed, without waiting for the queue.
     */
    byte @NotNull [] readScreenshot(final @NotNull Path runPath, final @NotNull String name) {
        if (!TestRunDirectoryDto.isScreenshotName(name)) return new byte[0];

        final @NotNull Path file = TestRunDirectoryDto.screenshotFile(runPath, name);
        return Optional.ofNullable(unwritten.get(file)).orElseGet(() -> Services.getInstance(p, TestDataFiles.class).readBytes(file));
    }

    /**
     * The run's marker, in the same queue as its results - so a status change and
     * the results it belongs to cannot land out of order.
     * <p>
     * <b>Through the marker writer, not into the file.</b> This wrote its own
     * bytes, which meant it alone skipped the two things every other marker write
     * does: it refused nothing, so a {@code .tr} that will not parse was replaced
     * by the defaults the scan fell back to - taking the run's status, its
     * execution stamps, its configuration and its result analysis with it, in one
     * status change the tester could not undo (#66, finding 162) - and it stamped
     * no folder id (Rule-INTERNAL-090). Both live in {@code MarkerFiles}, and one
     * writer is how they keep applying.
     * <p>
     * So this writes the marker the index holds rather than a snapshot, and the
     * marker is not a parameter for the same reason {@link
     * ProjectIndexer#changeRun} takes none: there is one marker per run and the
     * index has it. Ordering still holds - every change queues its own write, the
     * queue keeps them in order, and the last one writes the final state - and
     * nothing here is mutated in place: the two maps a tester writes into are
     * replaced whole by their setters.
     */
    void persistMarker(final @NotNull Path runPath) {
        queue.execute(() -> {
            try {
                // The same check the results write makes, for the same reason: a
                // run a sync or a delete took away while this sat in the queue is
                // not there to mark, and writing the marker recreated the folder
                // as a test run holding nothing (#312, N18).
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

    /**
     * The bytes to write, or nothing at all when the value cannot be written -
     * which is the one case where the queue must not be given work, because
     * there is nothing to put in the file and the file already holds something
     * true.
     */
    private @NotNull Optional<byte[]> snapshot(final @NotNull Object value, final @NotNull String what) {
        try {
            return Optional.of(Services.getInstance(p, Mapper.class).writeValueAsBytes(value));
        } catch (final Exception ex) {
            Logger.error("Failed to snapshot " + what + ": " + ex.getMessage());
            return Optional.empty();
        }
    }
}
