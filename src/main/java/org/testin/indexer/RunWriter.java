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
import org.testin.model.DirectoryType;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.markers.TestRunMarker;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * The one writer of a test run's two files: its results and its marker.
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
     * A new run's results, and the index entry that says the run exists.
     * <p>
     * <b>The cache is written here, the file there.</b> The registration used to
     * sit inside the queued task, so the worker mutated the index on its own
     * thread, outside any order the caller could see - and a caller that created
     * a run and read it back on the next line was racing the queue for it.
     */
    void create(final @NotNull Path runPath, final @NotNull TestRunDto tr) {
        store.registerTestRun(runPath, tr);
        write(runPath, tr);
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
    void persist(final @NotNull Path runPath, final @NotNull TestRunDto tr) {
        if (store.findTestRun(runPath).isEmpty()) {
            Logger.info("Test run no longer indexed, so it was not written back: " + runPath.getFileName());
            return;
        }

        store.registerTestRun(runPath, tr);
        write(runPath, tr);
    }

    /**
     * Between the submit and the execution the tester can delete the run, and
     * the queued task would then recreate {@code run.json} with its parent
     * directories: a run folder on disk with no marker. So the write asks again
     * whether the run is still indexed, which is the one question that separates
     * a pending write from a resurrection (#66, finding 86).
     */
    private void write(final @NotNull Path runPath, final @NotNull TestRunDto tr) {
        snapshot(tr, "test run data").ifPresent(bytes -> queue.execute(() -> {
            try {
                if (store.findTestRun(runPath).isEmpty()) {
                    Logger.info("Test run removed before its results were written, so nothing was written: " + runPath.getFileName());
                    return;
                }

                Services.getInstance(p, TestDataFiles.class).write(p, TestRunDirectoryDto.resultsFile(runPath), bytes);
                Logger.trace("Run results persisted for " + runPath.getFileName());
            } catch (final Exception ex) {
                Logger.error("Failed to persist test run data: " + ex.getMessage());
            }
        }));
    }

    /**
     * The run's marker, under the same discipline and in the same queue - so a
     * status change and the results it belongs to cannot land out of order.
     */
    void persistMarker(final @NotNull Path runPath, final @NotNull TestRunMarker marker) {
        snapshot(marker, "run marker").ifPresent(bytes -> queue.execute(() -> {
            try {
                Services.getInstance(p, TestDataFiles.class).write(p, runPath.resolve(DirectoryType.TR.getMarker()), bytes);
                Logger.trace("Marker persisted -> " + marker.getStatusLabel());
            } catch (final Exception ex) {
                Logger.error("Failed to persist marker: " + ex.getMessage());
            }
        }));
    }

    /**
     * UC-SHARE-019, Rule-SHARE-003.
     * <p>
     * Whether a file is one of the two this writer owns: a run's results or its
     * marker. Asked by a sync bringing files in, so a run's files that arrive from
     * a server join this queue instead of landing beside it.
     */
    boolean owns(final @NotNull Path file) {
        return file.endsWith(DirectoryType.TR.getMarker())
                || Optional.ofNullable(file.getParent()).map(TestRunDirectoryDto::resultsFile).filter(file::equals).isPresent();
    }

    /**
     * UC-SHARE-019, Rule-SHARE-003.
     * <p>
     * Writes bytes that arrived from a server into one of a run's files, in this
     * queue's order. Written straight to disk, they landed while a verdict write
     * for the same run was still queued, holding a snapshot taken before the
     * sync - and that write then put the older run back over them (#66, finding
     * 121).
     * <p>
     * No "is the run still indexed" question, unlike {@link #persist}: a run that
     * arrives from a server is not in the index until the scan that follows.
     */
    void write(final @NotNull Path file, final byte @NotNull [] bytes) {
        queue.execute(() -> {
            try {
                Services.getInstance(p, TestDataFiles.class).write(p, file, bytes);
            } catch (final Exception ex) {
                Logger.error("Failed to write an incoming run file " + file + ": " + ex.getMessage());
            }
        });
    }

    /**
     * UC-SHARE-019, Rule-SHARE-003.
     * <p>
     * Removes one of a run's files that a server no longer holds, in this queue's
     * order, and takes the run out of the index at once.
     * <p>
     * Deleted straight away, the file was gone while a write for the same run
     * was still queued, and that write put it back (#66, finding 130). Queued,
     * the deletion lands after anything already waiting. Out of the index, the
     * run answers {@link #persist}'s question with no, so a write that has not
     * started yet writes nothing. The scan that follows reads back whatever of
     * the run is left.
     */
    void delete(final @NotNull Path file, final @NotNull Path stopAt) {
        Optional.ofNullable(file.getParent()).ifPresent(store::removeTestRun);

        queue.execute(() -> {
            try {
                Services.getInstance(p, TestDataFiles.class).delete(p, file, stopAt);
            } catch (final Exception ex) {
                Logger.error("Failed to remove a run file the server no longer holds " + file + ": " + ex.getMessage());
            }
        });
    }

    /**
     * Returns once every write queued so far has landed, so a scan that follows
     * reads what was written instead of racing it.
     */
    void awaitQueued() {
        try {
            queue.submit(() -> {
            }).get();
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            Logger.warn("Interrupted while waiting for the run writer: " + ex.getMessage());
        } catch (final ExecutionException ex) {
            Logger.error("The run writer failed while it was waited for: " + ex.getMessage());
        }
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
