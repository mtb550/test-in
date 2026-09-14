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
import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.testin.model.DirectoryType;
import org.testin.model.Failure;
import org.testin.model.ResultAnalysis;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.markers.TestRunMarker;
import org.testin.services.Services;
import org.testin.testrun.RunStatusService;
import org.testin.util.Mapper;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * UC-SHARE-019.
 * <p>
 * A run's files that arrive from a server are not overwritten by a verdict
 * write that was already waiting in the run writer's queue.
 * <p>
 * An IDE test because the queue, the indexer and the file watcher's claim on
 * its own writes are all project services.
 */
public class RunWritesDuringSyncIdeTest extends BasePlatformTestCase {

    /**
     * How many writes of the older run are queued ahead of the sync - enough that
     * the queue is still working through them when the incoming file arrives.
     */
    private static final int QUEUED = 200;

    /**
     * The run's results file, by the path a server names it with.
     */
    private static final String RESULTS = DirectoryType.TRD.getFolderName() + "/Cycle 1/" + TestRunDirectoryDto.resultsFile(Path.of("")).getFileName();

    /**
     * How many other files the server dropped, deleted after the run's own - the
     * time a write held until the run's file is gone has to land in.
     */
    private static final int DROPPED = 200;

    private Path root;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = Files.createTempDirectory("testin-sync-runs");
    }

    @Override
    protected void tearDown() throws Exception {
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(each -> each.toFile().delete());
        } catch (final Exception ignored) {
            // Left for the operating system.
        } finally {
            super.tearDown();
        }
    }

    private ProjectIndexer indexer() {
        return Services.getInstance(getProject(), ProjectIndexer.class);
    }

    private TestProjectDirectoryDto testProject() {
        return WriteAction.computeAndWait(() -> {
            final TestProjectDirectoryDto tp = Services.getInstance(getProject(), DirectoryMapper.class).setTestProjectNode(getProject(), root.resolve("NAFATH"));
            indexer().addTestProject(tp);
            return tp;
        });
    }

    private static TestRunDto run(final String analysis) {
        final TestRunDto tr = new TestRunDto();
        tr.getResultAnalysis().put(ResultAnalysis.PASSED, analysis);
        return tr;
    }

    private static TestRunDto run(final String analysis, final UUID caseId) {
        final TestRunDto tr = run(analysis);
        tr.getResults().add(TestRunItems.builder().id(caseId).build());
        return tr;
    }

    private byte[] bytesOf(final TestRunDto tr) {
        try {
            return Services.getInstance(getProject(), Mapper.class).writeValueAsBytes(tr);
        } catch (final Exception ex) {
            throw new AssertionError("could not serialize the run", ex);
        }
    }

    /**
     * Rule-SHARE-003.
     * <p>
     * The sync wrote {@code run.json} straight to disk while verdict writes for
     * the same run were still queued, each holding a snapshot taken before the
     * sync - so they landed afterwards and put the older run back over the one
     * that had just arrived (#66, finding 121).
     */
    public void testAnIncomingRunIsNotOverwrittenByAWriteQueuedBeforeIt() {
        final TestProjectDirectoryDto tp = testProject();
        final Path runPath = tp.getTestRunsDirectory().getPath().resolve("Cycle 1");

        final TestRunDto older = run("written here before the sync");
        for (int i = 0; i < QUEUED; i++) indexer().putTestRun(runPath, older);

        indexer().acceptIncoming(tp.getPath(), Map.of(RESULTS, bytesOf(run("arrived from the server"))));

        final Path file = TestRunDirectoryDto.resultsFile(runPath);
        try {
            // Long enough for every queued write to have landed either way.
            Thread.sleep(3000);
            final String onDisk = Files.readString(file);

            assertTrue("a write queued before the sync put the older run back over the incoming one: " + onDisk,
                    onDisk.contains("arrived from the server"));
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while the queue drained", ex);
        } catch (final java.io.IOException ex) {
            throw new AssertionError("the run's results file is not there", ex);
        }
    }

    /**
     * Rule-SHARE-003.
     * <p>
     * A run the server no longer holds stays off disk once the sync removes it.
     * The removal deleted the file straight away while a write for the same run
     * was still queued, and that write then put the run back (#66, finding 130).
     * <p>
     * The window is narrow: the scan after the removal drops the run, and a
     * write that starts after that writes nothing. So the writer is held on a
     * latch and let go at the one moment that matters - once the file is gone,
     * or once the run has left the index, whichever the removal does first -
     * while the sync is still deleting everything else the server dropped.
     */
    public void testARemovedRunIsNotWrittenBackByAWriteQueuedBeforeIt() {
        final TestProjectDirectoryDto tp = testProject();
        final Path runPath = tp.getTestRunsDirectory().getPath().resolve("Cycle 1");
        final Path file = TestRunDirectoryDto.resultsFile(runPath);

        indexer().putTestRun(runPath, run("written here before the sync"));
        // Lands the write and reads the project, so the run is on disk and indexed.
        indexer().acceptIncoming(tp.getPath(), Map.of());

        final List<String> dropped = new ArrayList<>(List.of(RESULTS));
        try {
            for (int i = 0; i < DROPPED; i++) {
                final String note = "notes/note " + i + ".txt";
                Files.createDirectories(tp.getPath().resolve(note).getParent());
                Files.writeString(tp.getPath().resolve(note), "dropped by the server");
                dropped.add(note);
            }
        } catch (final java.io.IOException ex) {
            throw new AssertionError("could not write the files the server dropped", ex);
        }

        final CountDownLatch held = new CountDownLatch(1);
        writerQueue().execute(() -> awaitQuietly(held));
        indexer().putTestRun(runPath, run("written here before the sync"));

        final Future<?> release = ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final long giveUpAt = System.currentTimeMillis() + 10_000;
            while (Files.exists(file) && indexer().findTestRun(runPath).isPresent() && System.currentTimeMillis() < giveUpAt) {
                Thread.onSpinWait();
            }
            held.countDown();
        });

        indexer().removeIncoming(tp.getPath(), dropped);

        try {
            release.get(15, TimeUnit.SECONDS);
            // Lets anything still queued land before the file is looked for.
            indexer().acceptIncoming(tp.getPath(), Map.of());

            assertFalse("a write queued before the sync put the removed run back on disk", Files.exists(file));
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while the writer was held", ex);
        } catch (final ExecutionException | TimeoutException ex) {
            throw new AssertionError("the writer was never let go", ex);
        }
    }

    /**
     * Rule-SHARE-003.
     * <p>
     * A verdict recorded while a sync is bringing a newer {@code run.json} in
     * lands on the run that arrived. Between the incoming file landing and the
     * scan that reads it, the index still held the older run, so the verdict was
     * written with the older results over the file that had just arrived (#66,
     * finding 129).
     * <p>
     * The writer is held, so the sync is certainly still accepting when the
     * verdict is recorded: its incoming write waits on the latch, and the sync
     * waits on that write.
     */
    public void testAVerdictRecordedDuringASyncLandsOnTheRunThatArrived() {
        final TestProjectDirectoryDto tp = testProject();
        final Path runPath = tp.getTestRunsDirectory().getPath().resolve("Cycle 1");
        final UUID caseId = UUID.randomUUID();

        indexer().persistRunMarker(runPath, new TestRunMarker());
        indexer().putTestRun(runPath, run("written here before the sync", caseId));
        // Lands both writes and reads the project, so the run is on disk and indexed.
        indexer().acceptIncoming(tp.getPath(), Map.of());

        final CountDownLatch held = new CountDownLatch(1);
        writerQueue().execute(() -> awaitQuietly(held));

        final Thread sync = new Thread(() -> indexer().acceptIncoming(tp.getPath(), Map.of(RESULTS, bytesOf(run("arrived from the server", caseId)))), "sync under test");
        sync.start();

        try {
            awaitWaitingForTheWriter(sync);

            Services.getInstance(getProject(), RunStatusService.class).recordVerdict(getProject(), runPath, caseId, TestStatus.FAILED, Duration.ZERO, Failure.NONE);

            held.countDown();
            sync.join(15_000);
            // A held verdict is applied on the EDT, which this test runs on.
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            // Lets the verdict's write land.
            indexer().acceptIncoming(tp.getPath(), Map.of());

            final TestRunDto onDisk = Services.getInstance(getProject(), Mapper.class).readValue(TestRunDirectoryDto.resultsFile(runPath).toFile(), TestRunDto.class);

            assertEquals("the verdict put the older run back over the one that arrived", "arrived from the server", onDisk.getResultAnalysis().get(ResultAnalysis.PASSED));
            assertEquals("the verdict recorded during the sync was lost", TestStatus.FAILED, onDisk.getResults().getFirst().getStatus());
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while the sync ran", ex);
        } finally {
            held.countDown();
        }
    }

    /**
     * Returns once the sync is waiting for the run writer: past writing its
     * files, and not yet reading them back. Read off the thread's stack, because
     * that wait is the one moment this test needs and nothing announces it.
     */
    private static void awaitWaitingForTheWriter(final Thread sync) {
        final long giveUpAt = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < giveUpAt) {
            if (Arrays.stream(sync.getStackTrace()).anyMatch(frame -> frame.getMethodName().equals("awaitQueued"))) return;
            Thread.onSpinWait();
        }
        throw new AssertionError("the sync never reached the run writer");
    }

    /**
     * The run writer's queue, reached by reflection because nothing outside the
     * writer submits to it - and this test needs to hold it at one moment.
     */
    private ExecutorService writerQueue() {
        try {
            final Field writer = ProjectIndexer.class.getDeclaredField("runWriter");
            writer.setAccessible(true);
            final Field queue = RunWriter.class.getDeclaredField("queue");
            queue.setAccessible(true);
            return (ExecutorService) queue.get(writer.get(indexer()));
        } catch (final ReflectiveOperationException ex) {
            throw new AssertionError("the run writer's queue is not where this test looks for it", ex);
        }
    }

    private static void awaitQuietly(final CountDownLatch latch) {
        try {
            latch.await(15, TimeUnit.SECONDS);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
