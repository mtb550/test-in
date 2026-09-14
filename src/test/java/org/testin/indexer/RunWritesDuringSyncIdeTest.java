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

import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.testin.model.DirectoryType;
import org.testin.model.ResultAnalysis;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

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

        final String relative = DirectoryType.TRD.getFolderName() + "/Cycle 1/" + TestRunDirectoryDto.resultsFile(Path.of("")).getFileName();
        indexer().acceptIncoming(tp.getPath(), Map.of(relative, bytesOf(run("arrived from the server"))));

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
}
