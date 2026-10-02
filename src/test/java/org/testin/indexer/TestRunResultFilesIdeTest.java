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
import org.testin.TempTree;
import org.testin.model.FileKind;
import org.testin.model.TestRunItems;
import org.testin.model.RunItemStatus;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.NotNull;

public class TestRunResultFilesIdeTest extends BasePlatformTestCase {

    private static final UUID JUDGED_TEST_CASE = UUID.fromString("11111111-1111-4111-8111-111111111101");

    private static final UUID UNTICKED_TEST_CASE = UUID.fromString("11111111-1111-4111-8111-111111111102");

    private static final UUID PULLED_TEST_CASE = UUID.fromString("11111111-1111-4111-8111-111111111103");

    private static final String A_PULLED_RESULT = """
            {
              "id" : "11111111-1111-4111-8111-111111111103",
              "status" : "PASSED",
              "actualResult" : "Judged on the other machine"
            }""";

    private Path root;

    private static @NotNull Path resultOf(final Path testRunPath, final UUID testCaseId) {
        return testRunPath.resolve(FileKind.RUN_ITEM.fileName(testCaseId));
    }

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

    private static @NotNull String read(final Path file) {
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
            throw new AssertionError("could not put " + file.getFileName() + " in the test run's folder", ex);
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
            TempTree.delete(root);
        } finally {
            super.tearDown();
        }
    }

    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull Path aTestRun() {
        final Path testRunPath = WriteAction.computeAndWait(() -> {
            final DirectoryMapper mapper = Services.getInstance(getProject(), DirectoryMapper.class);

            final TestProjectDirectoryDto tp = mapper.setTestProjectNode(root.resolve("NAFATH"));
            nodes().addTestProject(tp);

            final Path path = tp.getTestRunsDirectory().getPath().resolve("Cycle-1");
            final TestRunDirectoryDto tr = mapper.setTestRunNode(path, tp.getTestRunsDirectory());
            nodes().addTestRunDir(tr);
            return path;
        });

        final List<TestRunItems> results = new ArrayList<>();
        for (final UUID testCaseId : List.of(JUDGED_TEST_CASE, UNTICKED_TEST_CASE)) {
            results.add(new TestRunItems().setId(testCaseId).setStatus(RunItemStatus.PASSED));
        }

        indexedTestRuns().putTestRun(testRunPath, new TestRunDto().setResults(results));
        return testRunPath;
    }

    public void testEachTestCaseTheTestRunCoversGetsAFileOfItsOwn() {
        final Path testRun = aTestRun();

        awaitFile(resultOf(testRun, JUDGED_TEST_CASE), "the test run's results never reached disk");
        awaitFile(resultOf(testRun, UNTICKED_TEST_CASE), "a test case the test run covers has no result file");
    }

    public void testUntickingATestCaseRemovesItsResultAndLeavesEverythingElse() {
        final Path testRun = aTestRun();
        awaitFile(resultOf(testRun, UNTICKED_TEST_CASE), "the test run's results never reached disk");

        writePulledResult(resultOf(testRun, PULLED_TEST_CASE));

        indexedTestRuns().changeTestRun(testRun, tr -> tr.setResults(new ArrayList<>(tr.getResults().stream()
                .filter(item -> item.getId().equals(JUDGED_TEST_CASE))
                .toList())));

        await("the result of the test case the tester unticked is still in the test run's folder",
                () -> !Files.exists(resultOf(testRun, UNTICKED_TEST_CASE)));

        assertTrue("the result of a test case the change kept went with the one it dropped",
                Files.isRegularFile(resultOf(testRun, JUDGED_TEST_CASE)));

        assertTrue("a result the test run does not cover was swept away although nothing unticked it - which is how a"
                        + " run item status a pull had just brought, or one whose own write failed, was lost",
                Files.isRegularFile(resultOf(testRun, PULLED_TEST_CASE)));
        assertEquals("and the run item status it carries was rewritten rather than left alone",
                A_PULLED_RESULT, read(resultOf(testRun, PULLED_TEST_CASE)));
    }

    public void testARunItemStatusWritesTheResultItChangedAndNoOther() {
        final Path testRun = aTestRun();
        awaitFile(resultOf(testRun, UNTICKED_TEST_CASE), "the test run's results never reached disk");
        final String untouched = read(resultOf(testRun, UNTICKED_TEST_CASE));

        indexedTestRuns().findTestRun(testRun).orElseThrow().resultOf(UNTICKED_TEST_CASE).orElseThrow().setActualResult("Not saved yet");
        indexedTestRuns().changeResult(testRun, JUDGED_TEST_CASE, item -> item.setStatus(RunItemStatus.FAILED));

        await("the run item status never reached its result file", () -> read(resultOf(testRun, JUDGED_TEST_CASE)).contains("FAILED"));
        assertEquals("a run item status on one test case rewrote the result of another", untouched, read(resultOf(testRun, UNTICKED_TEST_CASE)));
    }
}
