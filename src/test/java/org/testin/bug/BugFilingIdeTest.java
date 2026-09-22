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
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.TimeoutUtil;
import org.testin.TempTree;
import org.testin.indexer.DirectoryMapper;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.FileKind;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
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

public class BugFilingIdeTest extends BasePlatformTestCase {

    private static final String ISSUE = "https://github.com/mtb550/test-in/issues/412";

    private Path root;

    private static String awaitFileHoldingTheIssue(final Path file) {
        final long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            final String held = read(file);
            if (held.contains(ISSUE)) return held;

            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            TimeoutUtil.sleep(20);
        }
        return read(file);
    }

    private static String read(final Path file) {
        try {
            return Files.exists(file) ? Files.readString(file) : "";
        } catch (final IOException beingWritten) {
            return "";
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = Files.createTempDirectory("testin-bug-filing");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (root != null) TempTree.delete(root);
        } finally {
            super.tearDown();
        }
    }

    private ProjectIndexer indexer() {
        return Services.getInstance(getProject(), ProjectIndexer.class);
    }

    private Path runPath() {
        return root.resolve("NAFATH").resolve("Test Runs").resolve("Sprint 7");
    }

    private BugReports.RunItem indexedRunItem(final TestStatus status) {
        return runItem(indexedTestCase(), status);
    }

    private BugReports.RunItem runItem(final UUID testCaseId, final TestStatus status) {
        final TestRunItems item = TestRunItems.builder().id(testCaseId).status(status).build();
        indexer().putTestRun(runPath(), TestRunDto.builder().results(new ArrayList<>(List.of(item))).build());
        return new BugReports.RunItem(runPath(), testCaseId);
    }

    private UUID indexedTestCase() {
        final TestSetDirectoryDto ts = WriteAction.computeAndWait(() -> {
            final DirectoryMapper mapper = Services.getInstance(getProject(), DirectoryMapper.class);
            final TestProjectDirectoryDto tp = mapper.setTestProjectNode(getProject(), root.resolve("NAFATH"));
            indexer().addTestProject(tp);

            final TestSetDirectoryDto set = mapper.getTestSetNode(getProject(), tp.getTestCasesDirectory().getPath().resolve("Login"), tp.getTestCasesDirectory());
            indexer().addTestSet(set);
            return set;
        });

        final TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).description("Log in with a valid user").build();
        tc.setParent(ts);
        indexer().putTestCase(ts.getPath(), tc);
        return tc.getId();
    }

    private String storedLink(final BugReports.RunItem item) {
        return indexer().findTestRun(item.run()).flatMap(item::in).orElseThrow().getBugIssueUrl();
    }

    public void testAFailedRunItemKeepsTheIssue() {
        final BugReports.RunItem item = indexedRunItem(TestStatus.FAILED);

        assertEquals(Optional.empty(), BugFiling.store(getProject(), item, ISSUE));
        assertEquals("the run item the indexer holds did not take the link", ISSUE, storedLink(item));

        final Path result = item.run().resolve(FileKind.RUN_ITEM.fileName(item.id()));
        assertTrue("the link did not reach the case's own result file", awaitFileHoldingTheIssue(result).contains(ISSUE));
    }

    public void testARunItemNoLongerFailedIsNotWritten() {
        final BugReports.RunItem item = indexedRunItem(TestStatus.PASSED);

        assertEquals(Optional.of(Bundle.message("bug.not.stored.no.longer.failed")), BugFiling.store(getProject(), item, ISSUE));
        assertEquals("a passed run item was given a bug", "", storedLink(item));
    }

    public void testARunRenamedOrRemovedIsNotBroughtBack() {
        final BugReports.RunItem gone = new BugReports.RunItem(runPath(), UUID.randomUUID());

        assertEquals(Optional.of(Bundle.message("bug.not.stored.moved")), BugFiling.store(getProject(), gone, ISSUE));
        assertTrue("storing on the old path registered the run again", indexer().findTestRun(runPath()).isEmpty());
    }

    public void testARemovedRunItemIsNotWritten() {
        final BugReports.RunItem item = indexedRunItem(TestStatus.REMOVED);

        assertEquals(Optional.of(Bundle.message("bug.not.stored.moved")), BugFiling.store(getProject(), item, ISSUE));
    }

    public void testARunItemWhoseTestCaseIsGoneIsNotWritten() {
        final BugReports.RunItem item = runItem(UUID.randomUUID(), TestStatus.FAILED);

        assertEquals(Optional.of(Bundle.message("bug.not.stored.moved")), BugFiling.store(getProject(), item, ISSUE));
    }
}
