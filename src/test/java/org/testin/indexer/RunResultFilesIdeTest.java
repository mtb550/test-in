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
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.TimeoutUtil;
import org.testin.model.FileKind;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * UC-INTERNAL-005, UC-TREE-PANEL-022, Rule-INTERNAL-011.
 * <p>
 * What a run's write leaves in its folder: one {@code <test case id>.ri} per
 * case it covers, and nothing else of its doing (#305, S21).
 * <p>
 * <b>The regression this pins is what a write removes.</b> The writer used to
 * work out which results were unwanted by listing the folder and dropping every
 * {@code .ri} the snapshot did not name. So a result a pull had brought a
 * moment earlier, or one whose own snapshot had just failed, looked exactly like
 * a case somebody had unticked. A verdict nobody asked to lose was deleted.
 * The caller says which cases went, because the caller is the only one that saw
 * the change, and the writer removes those and no others.
 * <p>
 * An IDE test because the writer is reached through the indexer, writes through
 * the project's own services and claims its writes with the file watcher; there
 * is no seam that answers the question without them. The writes are queued, so
 * every assertion waits for the queue rather than assuming it has run.
 */
public class RunResultFilesIdeTest extends BasePlatformTestCase {

    private static final UUID JUDGED_CASE = UUID.fromString("11111111-1111-4111-8111-111111111101");

    private static final UUID UNTICKED_CASE = UUID.fromString("11111111-1111-4111-8111-111111111102");

    /**
     * A case this run never covered, whose result is in the folder all the same:
     * what a pull brings while the tester has the run open.
     */
    private static final UUID PULLED_CASE = UUID.fromString("11111111-1111-4111-8111-111111111103");

    private static final String A_PULLED_RESULT = """
            {
              "id" : "11111111-1111-4111-8111-111111111103",
              "status" : "PASSED",
              "actualResult" : "Judged on the other machine"
            }""";

    private Path root;

    private static void deleteTree(final Path path) {
        if (path == null) return;

        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(each -> {
                try {
                    Files.deleteIfExists(each);
                } catch (final Exception ignored) {
                    // Left for the operating system.
                }
            });
        } catch (final Exception ignored) {
            // Nothing to walk, or nothing to remove.
        }
    }

    private static Path resultOf(final Path runPath, final UUID testCaseId) {
        return runPath.resolve(FileKind.RUN_ITEM.fileName(testCaseId));
    }

    /**
     * Waits for a queued write to land, dispatching events meanwhile in case the
     * writer needs this thread. A file is created before its bytes land, so what
     * is waited for is a file with something in it (#312, N21).
     */
    private static void awaitFile(final Path file, final String what) {
        await(what + ": " + file.getFileName(), () -> !read(file).isEmpty());
    }

    private static void await(final String what, final BooleanSupplier landed) {
        final long deadline = System.currentTimeMillis() + 10_000;

        while (System.currentTimeMillis() < deadline) {
            if (landed.getAsBoolean()) return;

            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            TimeoutUtil.sleep(20);
        }

        fail(what);
    }

    /**
     * The file's text, and nothing while it does not exist or is being written.
     */
    private static String read(final Path file) {
        try {
            return Files.exists(file) ? Files.readString(file) : "";
        } catch (final IOException beingWritten) {
            return "";
        }
    }

    private static void writePulledResult(final Path file) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, A_PULLED_RESULT);
        } catch (final IOException ex) {
            throw new AssertionError("could not put " + file.getFileName() + " in the run's folder", ex);
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = Files.createTempDirectory("testin-run-results");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            deleteTree(root);
        } finally {
            super.tearDown();
        }
    }

    private ProjectIndexer indexer() {
        return Services.getInstance(getProject(), ProjectIndexer.class);
    }

    /**
     * A run the indexer holds, covering the judged and the unticked case, built the way Create Test
     * Run builds one: the mapper makes the nodes, the indexer is told, and the
     * run is put in through the one door that registers it.
     */
    private Path aRun() {
        final Path runPath = WriteAction.computeAndWait(() -> {
            final DirectoryMapper mapper = Services.getInstance(getProject(), DirectoryMapper.class);

            final TestProjectDirectoryDto tp = mapper.setTestProjectNode(getProject(), root.resolve("NAFATH"));
            indexer().addTestProject(tp);

            final Path path = tp.getTestRunsDirectory().getPath().resolve("Cycle-1");
            final TestRunDirectoryDto tr = mapper.setTestRunNode(getProject(), path, tp.getTestRunsDirectory());
            indexer().addTestRunDir(tr);
            return path;
        });

        final List<TestRunItems> results = new ArrayList<>();
        for (final UUID testCaseId : List.of(JUDGED_CASE, UNTICKED_CASE)) {
            results.add(new TestRunItems().setId(testCaseId).setStatus(TestStatus.PASSED));
        }

        indexer().putTestRun(runPath, new TestRunDto().setResults(results));
        return runPath;
    }

    /**
     * Rule-INTERNAL-011, Rule-INTERNAL-012. One file per result, named by the
     * case it is about - so recording a verdict writes one file, and two testers
     * judging different cases of one cycle never touch the same file.
     */
    public void testEachCaseTheRunCoversGetsAFileOfItsOwn() {
        final Path run = aRun();

        awaitFile(resultOf(run, JUDGED_CASE), "the run's results never reached disk");
        awaitFile(resultOf(run, UNTICKED_CASE), "a case the run covers has no result file");
    }

    /**
     * UC-TREE-PANEL-022, Rule-INTERNAL-011.
     * <p>
     * Unticking a case in Edit Test Run removes that case's result, and only
     * that one. The result a pull brought is not in the run the index holds, and
     * that is not evidence anybody stopped covering it - the old sweep read it
     * as unwanted and deleted a verdict recorded on another machine (#305).
     */
    public void testUntickingACaseRemovesItsResultAndLeavesEverythingElse() {
        final Path run = aRun();
        awaitFile(resultOf(run, UNTICKED_CASE), "the run's results never reached disk");

        writePulledResult(resultOf(run, PULLED_CASE));

        indexer().changeRun(run, tr -> tr.setResults(new ArrayList<>(tr.getResults().stream()
                .filter(item -> item.getId().equals(JUDGED_CASE))
                .toList())));

        await("the result of the case the tester unticked is still in the run's folder",
                () -> !Files.exists(resultOf(run, UNTICKED_CASE)));

        assertTrue("the result of a case the change kept went with the one it dropped",
                Files.isRegularFile(resultOf(run, JUDGED_CASE)));

        assertTrue("a result the run does not cover was swept away although nothing unticked it - which is how a"
                        + " verdict a pull had just brought, or one whose own write failed, was lost",
                Files.isRegularFile(resultOf(run, PULLED_CASE)));
        assertEquals("and the verdict it carries was rewritten rather than left alone",
                A_PULLED_RESULT, read(resultOf(run, PULLED_CASE)));
    }
}
