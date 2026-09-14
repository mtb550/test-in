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
import org.testin.indexer.DirectoryMapper;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Recording the issue a bug report became (#28): only on a run and a run item
 * found again through the indexer, and only while the run item is still failed.
 * <p>
 * An IDE test because the run is the indexer's, and the write goes through its
 * run writer.
 */
public class BugFilingIdeTest extends BasePlatformTestCase {

    private static final String ISSUE = "https://github.com/mtb550/test-in/issues/412";

    private Path root;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = Files.createTempDirectory("testin-bug-filing");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            deleteTree(root);
        } finally {
            super.tearDown();
        }
    }

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

    private ProjectIndexer indexer() {
        return Services.getInstance(getProject(), ProjectIndexer.class);
    }

    private Path runPath() {
        return root.resolve("NAFATH").resolve("Test Runs").resolve("Sprint 7");
    }

    /**
     * A run the indexer holds, with one run item in it whose test case the
     * indexer holds too: a run item whose test case is gone is removed, and
     * takes no link (#66, finding 110).
     */
    private BugReports.RunItem indexedRunItem(final TestStatus status) {
        return runItem(indexedCase(), status);
    }

    private BugReports.RunItem runItem(final UUID caseId, final TestStatus status) {
        final TestRunItems item = TestRunItems.builder().id(caseId).status(status).build();
        indexer().registerTestRun(runPath(), TestRunDto.builder().results(new ArrayList<>(List.of(item))).build());
        return new BugReports.RunItem(runPath(), caseId);
    }

    private UUID indexedCase() {
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

    /**
     * The write is queued, so the test waits for it rather than deleting the
     * folder under it - dispatching events meanwhile, in case the writer needs
     * this thread.
     */
    private static void awaitFile(final Path file) {
        final long deadline = System.currentTimeMillis() + 10_000;
        while (!Files.exists(file) && System.currentTimeMillis() < deadline) {
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            TimeoutUtil.sleep(20);
        }
    }

    public void testAFailedRunItemKeepsTheIssue() {
        final BugReports.RunItem item = indexedRunItem(TestStatus.FAILED);

        assertEquals(Optional.empty(), BugFiling.store(getProject(), item, ISSUE));
        assertEquals("the run item the indexer holds did not take the link", ISSUE, storedLink(item));

        final Path results = TestRunDirectoryDto.resultsFile(item.run());
        awaitFile(results);
        try {
            assertTrue("the link did not reach the run's results file", Files.readString(results).contains(ISSUE));
        } catch (final java.io.IOException ex) {
            throw new AssertionError("the run's results were not written", ex);
        }
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

    /**
     * #66, finding 110: a failed run item whose test case is no longer indexed is
     * removed, though its file still says Failed, so it takes no link.
     */
    public void testARunItemWhoseTestCaseIsGoneIsNotWritten() {
        final BugReports.RunItem item = runItem(UUID.randomUUID(), TestStatus.FAILED);

        assertEquals(Optional.of(Bundle.message("bug.not.stored.moved")), BugFiling.store(getProject(), item, ISSUE));
    }
}
