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

package org.testin.bug;

import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.TimeoutUtil;
import org.testin.AbstractTempRootIdeTest;
import org.testin.indexer.DirectoryMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.FileKind;
import org.testin.model.TestRunItems;
import org.testin.model.RunItemStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class BugFilingIdeTest extends AbstractTempRootIdeTest {

    private static final String ISSUE = "https://github.com/mtb550/test-in/issues/412";

    private static @NotNull String awaitFileHoldingTheIssue(final Path file) {
        final long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            final String held = read(file);
            if (held.contains(ISSUE)) return held;

            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            TimeoutUtil.sleep(20);
        }
        return read(file);
    }

    private static @NotNull String read(final Path file) {
        try {
            return Files.exists(file) ? Files.readString(file) : "";
        } catch (final IOException beingWritten) {
            return "";
        }
    }

    private @NotNull TestCases indexedTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull Path sprint7TestRunPath() {
        return root.resolve("NAFATH").resolve("Test Runs").resolve("Sprint 7");
    }

    private @NotNull RunItem indexedRunItem(final RunItemStatus status) {
        return runItem(indexedTestCase(), status);
    }

    private @NotNull RunItem runItem(final UUID testCaseId, final RunItemStatus status) {
        final TestRunItems item = TestRunItems.builder().id(testCaseId).status(status).build();
        indexedTestRuns().putTestRun(sprint7TestRunPath(), TestRunDto.builder().results(new ArrayList<>(List.of(item))).build());
        return new RunItem(sprint7TestRunPath(), testCaseId);
    }

    private @NotNull UUID indexedTestCase() {
        final TestSetDirectoryDto ts = WriteAction.computeAndWait(() -> {
            final DirectoryMapper mapper = Services.getInstance(getProject(), DirectoryMapper.class);
            final TestProjectDirectoryDto tp = mapper.setTestProjectNode(root.resolve("NAFATH"));
            nodes().addTestProject(tp);

            final TestSetDirectoryDto set = mapper.getTestSetNode(tp.getTestCasesDirectory().getPath().resolve("Login"), tp.getTestCasesDirectory());
            nodes().addTestSet(set);
            return set;
        });

        final TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).description("Log in with a valid user").build();
        tc.setParent(ts);
        indexedTestCases().putTestCase(ts.getPath(), tc);
        return tc.getId();
    }

    private @NotNull String storedLink(final RunItem item) {
        return indexedTestRuns().findTestRun(item.testRunPath()).flatMap(item::in).orElseThrow().getBugIssueUrl();
    }

    public void testAFailedRunItemKeepsTheIssue() {
        final RunItem item = indexedRunItem(RunItemStatus.FAILED);

        assertEquals(Optional.empty(), BugFiling.store(getProject(), item, ISSUE));
        assertEquals("the run item the indexer holds did not take the link", ISSUE, storedLink(item));

        final Path result = item.testRunPath().resolve(FileKind.RUN_ITEM.fileName(item.id()));
        assertTrue("the link did not reach the test case's own result file", awaitFileHoldingTheIssue(result).contains(ISSUE));
    }

    public void testARunItemNoLongerFailedIsNotWritten() {
        final RunItem item = indexedRunItem(RunItemStatus.PASSED);

        assertEquals(Optional.of(Bundle.message("bug.not.stored.no.longer.failed")), BugFiling.store(getProject(), item, ISSUE));
        assertEquals("a passed run item was given a bug", "", storedLink(item));
    }

    public void testATestRunRenamedOrRemovedIsNotBroughtBack() {
        final RunItem gone = new RunItem(sprint7TestRunPath(), UUID.randomUUID());

        assertEquals(Optional.of(Bundle.message("bug.not.stored.moved")), BugFiling.store(getProject(), gone, ISSUE));
        assertTrue("storing on the old path registered the test run again", indexedTestRuns().findTestRun(sprint7TestRunPath()).isEmpty());
    }

    public void testARemovedRunItemIsNotWritten() {
        final RunItem item = indexedRunItem(RunItemStatus.REMOVED);

        assertEquals(Optional.of(Bundle.message("bug.not.stored.moved")), BugFiling.store(getProject(), item, ISSUE));
    }

    public void testARunItemWhoseTestCaseIsGoneIsNotWritten() {
        final RunItem item = runItem(UUID.randomUUID(), RunItemStatus.FAILED);

        assertEquals(Optional.of(Bundle.message("bug.not.stored.moved")), BugFiling.store(getProject(), item, ISSUE));
    }
}
