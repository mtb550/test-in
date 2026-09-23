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
import org.testin.model.TestStatus;
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

public class RunResultFilesIdeTest extends BasePlatformTestCase {

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

    private static Path resultOf(final Path runPath, final UUID testCaseId) {
        return runPath.resolve(FileKind.RUN_ITEM.fileName(testCaseId));
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
            TempTree.delete(root);
        } finally {
            super.tearDown();
        }
    }

    private ProjectIndexer indexer() {
        return Services.getInstance(getProject(), ProjectIndexer.class);
    }

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
        for (final UUID testCaseId : List.of(JUDGED_TEST_CASE, UNTICKED_TEST_CASE)) {
            results.add(new TestRunItems().setId(testCaseId).setStatus(TestStatus.PASSED));
        }

        indexer().putTestRun(runPath, new TestRunDto().setResults(results));
        return runPath;
    }

    public void testEachTestCaseTheRunCoversGetsAFileOfItsOwn() {
        final Path run = aRun();

        awaitFile(resultOf(run, JUDGED_TEST_CASE), "the run's results never reached disk");
        awaitFile(resultOf(run, UNTICKED_TEST_CASE), "a case the run covers has no result file");
    }

    public void testUntickingATestCaseRemovesItsResultAndLeavesEverythingElse() {
        final Path run = aRun();
        awaitFile(resultOf(run, UNTICKED_TEST_CASE), "the run's results never reached disk");

        writePulledResult(resultOf(run, PULLED_TEST_CASE));

        indexer().changeRun(run, tr -> tr.setResults(new ArrayList<>(tr.getResults().stream()
                .filter(item -> item.getId().equals(JUDGED_TEST_CASE))
                .toList())));

        await("the result of the case the tester unticked is still in the run's folder",
                () -> !Files.exists(resultOf(run, UNTICKED_TEST_CASE)));

        assertTrue("the result of a case the change kept went with the one it dropped",
                Files.isRegularFile(resultOf(run, JUDGED_TEST_CASE)));

        assertTrue("a result the run does not cover was swept away although nothing unticked it - which is how a"
                        + " verdict a pull had just brought, or one whose own write failed, was lost",
                Files.isRegularFile(resultOf(run, PULLED_TEST_CASE)));
        assertEquals("and the verdict it carries was rewritten rather than left alone",
                A_PULLED_RESULT, read(resultOf(run, PULLED_TEST_CASE)));
    }
}
