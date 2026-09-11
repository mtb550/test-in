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
     * The run's results, and the index entry that says the run exists.
     * <p>
     * The registration is inside the queued write because it is what the file
     * describes: an index entry for a run whose JSON never landed would be a
     * run the tree offers and the disk does not have.
     */
    void persist(final @NotNull Path runPath, final @NotNull TestRunDto tr) {
        snapshot(tr, "test run data").ifPresent(bytes -> queue.execute(() -> {
            try {
                store.registerTestRun(runPath, tr);
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
