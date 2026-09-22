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
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.testin.TempTree;
import org.testin.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UC-INTERNAL-003, Rule-INTERNAL-021.
 * <p>
 * Reading a test project again never stops the index holding it.
 * <p>
 * The scan emptied the project out before the pass that read it back. So for as
 * long as the walk took - a real project is thousands of files - the index had
 * no record of a project that was on disk the whole time. A rescan is a Git
 * pull, a branch switch, a hand edit or Refresh, and every one of them happens
 * while a tester is working. In that window the lookups that treat a miss as a
 * mistake in the plugin met one. P or F or B on a row that was not executing
 * raised an internal error. A verdict on the executing row was dropped while
 * the editor went on saying Passed. A test case saved then was stamped as
 * created by whoever was watching (#312, A1).
 * <p>
 * An IDE test because the indexer is a project service, and a threaded one
 * because the defect is a window rather than a result: what has to be true is
 * that nothing disappears <b>while</b> the pass runs, which no single call can
 * answer.
 */
public class RescanKeepsTheIndexIdeTest extends BasePlatformTestCase {

    /**
     * Big enough that the second pass takes long enough to catch in the act, and
     * small enough that the test is not itself the slow one. Before the fix the
     * project is absent for very nearly all of it.
     */
    private static final int SETS = 60;
    private static final int CASES_PER_SET = 10;

    private static final int SCAN_TIMEOUT_SECONDS = 120;

    private Path root;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = Files.createTempDirectory("testin-rescan");
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

    /**
     * Rule-INTERNAL-021.
     * <p>
     * A test set and a test case that are on disk throughout are in the index
     * throughout.
     */
    public void testNothingDisappearsWhileTheProjectIsReadAgain() {
        final Path project = SyntheticTree.write(root, SETS, CASES_PER_SET);
        indexer().scanSingleProject(project);

        final Path set = project.resolve("Test Cases").resolve("set-0");
        final UUID testCase = indexer().getTestCasesForTestSet(set).getFirst().getId();

        assertTrue("the first pass did not index the project, so there is nothing to watch",
                indexer().nodeExists(set) && indexer().findTestCase(testCase).isPresent());

        final AtomicBoolean vanished = new AtomicBoolean(false);
        final CountDownLatch scanned = new CountDownLatch(1);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                indexer().scanSingleProject(project);
            } finally {
                scanned.countDown();
            }
        });

        while (scanned.getCount() > 0) {
            if (!indexer().nodeExists(set) || indexer().findTestCase(testCase).isEmpty()) vanished.set(true);
            Thread.onSpinWait();
        }

        await(scanned);

        assertFalse("the project was emptied out of the index while it was being read again, so every "
                        + "surface asking about it in that window was answered as though it had been deleted",
                vanished.get());

        assertTrue("and it is there when the pass has finished",
                indexer().nodeExists(set) && indexer().findTestCase(testCase).isPresent());
    }

    /**
     * Rule-INTERNAL-021.
     * <p>
     * What a rescan is for: a test set deleted while the plugin was not looking
     * is gone from the index, with its cases, rather than left in the tree,
     * global search, the completion values and every export until somebody
     * presses Refresh (#66, finding 68).
     * <p>
     * Here because the swap that keeps the index full is the same code that
     * empties what is no longer there: the one must not have cost the other.
     */
    public void testASetDeletedOnDiskIsGoneAfterTheRescan() {
        final Path project = SyntheticTree.write(root, 3, CASES_PER_SET);
        indexer().scanSingleProject(project);

        final Path deleted = project.resolve("Test Cases").resolve("set-1");
        final Path kept = project.resolve("Test Cases").resolve("set-2");

        final UUID caseInDeleted = indexer().getTestCasesForTestSet(deleted).getFirst().getId();
        final UUID caseInKept = indexer().getTestCasesForTestSet(kept).getFirst().getId();

        assertTrue("could not delete " + deleted, TempTree.delete(deleted));
        indexer().scanSingleProject(project);

        assertFalse("a test set deleted on disk is still in the index after a rescan",
                indexer().nodeExists(deleted));
        assertTrue("its test cases went with it", indexer().findTestCase(caseInDeleted).isEmpty());
        assertTrue("the test cases of a set nobody touched went too", indexer().findTestCase(caseInKept).isPresent());
        assertTrue("and the sets that are still there are still there", indexer().nodeExists(kept));
    }

    private void await(final CountDownLatch latch) {
        try {
            assertTrue("the rescan did not finish", latch.await(SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while the rescan ran", interrupted);
        }
    }
}
