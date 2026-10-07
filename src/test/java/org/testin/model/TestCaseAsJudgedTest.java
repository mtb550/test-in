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

package org.testin.model;

import org.jetbrains.annotations.NotNull;
import org.testin.model.node.TestSetDirectoryDto;
import org.testin.model.result.TestRunItems;
import org.testin.model.status.RunItemStatus;
import org.testin.util.Mapper;
import org.testin.util.RealMapper;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class TestCaseAsJudgedTest {
    private static final Mapper MAPPER = RealMapper.build();

    private static @NotNull TestCaseDto testCaseReading(final UUID id, final String description) {
        return TestCaseDto.builder().id(id).description(description).build();
    }

    private static @NotNull TestRunItems passed(final UUID id) {
        final TestRunItems item = TestRunItems.builder().id(id).build();
        item.recordRunItemStatus(RunItemStatus.PASSED, "tester");
        return item;
    }

    // Rule-EDITOR-PANEL-239
    @Test
    public void aRunItemOfATestRunNotCommittedShowsEveryEdit() {
        final UUID id = UUID.randomUUID();
        final TestRunItems item = passed(id);

        item.showing(Optional.of(testCaseReading(id, "after")), Optional.empty(), Optional.empty());

        assertEquals(item.shownTestCase().getDescription(), "after", "a test run not committed shows the test case as it is now");
    }

    // Rule-EDITOR-PANEL-239
    @Test
    public void aRunItemOfACommittedTestRunShowsTheTestCaseFromItsCommit() {
        final UUID id = UUID.randomUUID();
        final TestRunItems item = passed(id);

        item.showing(Optional.of(testCaseReading(id, "after")), Optional.of(testCaseReading(id, "before")), Optional.empty());

        assertEquals(item.shownTestCase().getDescription(), "before", "the run item shows what its commit holds");
        assertEquals(item.liveTestCase().getDescription(), "after", "actions still reach the test case as it is now");
    }

    // Rule-EDITOR-PANEL-240
    @Test
    public void aCorrectionChangesOnlyTheRunItemStatusWhoAndWhen() {
        final UUID id = UUID.randomUUID();
        final TestRunItems item = passed(id);
        item.showing(Optional.of(testCaseReading(id, "now")), Optional.empty(), Optional.empty());

        item.recordRunItemStatus(RunItemStatus.FAILED, "other tester");

        assertEquals(item.getStatus(), RunItemStatus.FAILED);
        assertEquals(item.getExecutedBy(), "other tester");
        assertEquals(item.shownTestCase().getDescription(), "now");
    }

    // Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239
    @Test
    public void aTestCaseDeletedFromATestRunNotCommittedReadsRemovedWithItsLastTextInGit() {
        final UUID id = UUID.randomUUID();
        final TestRunItems item = passed(id);

        item.showing(Optional.empty(), Optional.empty(), Optional.of(testCaseReading(id, "as Git last held it")));

        assertTrue(item.isRemoved(), "nothing may act on the run item of a test case that no longer exists");
        assertEquals(item.shownStatus(), RunItemStatus.REMOVED, "a deleted test case reads Removed while its test run is not Committed");
        assertEquals(item.getStatus(), RunItemStatus.PASSED, "the file keeps the run item status it was given");
        assertEquals(item.shownTestCase().getDescription(), "as Git last held it");
    }

    // Rule-EDITOR-PANEL-126
    @Test
    public void aTestCaseDeletedBeforeGitEverHeldItShowsThePlaceholder() {
        final UUID id = UUID.randomUUID();
        final TestRunItems item = passed(id);

        item.showing(Optional.empty(), Optional.empty(), Optional.empty());

        assertEquals(item.shownTestCase().getDescription(), TestCaseDto.deleted(id).getDescription());
    }

    // Rule-EDITOR-PANEL-239
    @Test
    public void aTestCaseDeletedFromACommittedTestRunKeepsTheTextOfItsCommit() {
        final UUID id = UUID.randomUUID();
        final TestRunItems item = passed(id);

        item.showing(Optional.empty(), Optional.of(testCaseReading(id, "before")), Optional.empty());

        assertFalse(item.isRemoved(), "a committed test run keeps every run item it recorded");
        assertEquals(item.shownStatus(), RunItemStatus.PASSED);
        assertEquals(item.shownTestCase().getDescription(), "before");
    }

    @Test
    public void theCommittedTestCaseTakesItsTestSetFromTheLiveTestCase() {
        final UUID id = UUID.randomUUID();
        final TestSetDirectoryDto set = new TestSetDirectoryDto();
        final TestRunItems item = passed(id);

        item.showing(Optional.of(TestCaseDto.builder().id(id).parent(set).build()), Optional.of(testCaseReading(id, "before")), Optional.empty());

        assertSame(item.shownTestCase().getParent(), set, "where the test case sits is the live test case's, so navigation still works");
    }

    // Rule-EDITOR-PANEL-238
    @Test
    public void aRunItemIsWrittenWithoutATestCase() {
        final UUID id = UUID.randomUUID();
        final TestRunItems item = passed(id);
        item.showing(Optional.of(testCaseReading(id, "now")), Optional.of(testCaseReading(id, "before")), Optional.empty());

        final String written = MAPPER.writeValueAsString(item);

        assertFalse(written.contains("testCase"), "a run item keeps no copy of its test case: " + written);
    }

    // Rule-EDITOR-PANEL-238
    @Test
    public void aRunItemWrittenWithACopyIsReadWithoutIt() {
        final UUID id = UUID.randomUUID();
        final TestRunItems read = MAPPER.readValue("{\"id\":\"" + id + "\",\"status\":\"PASSED\",\"testCase\":{\"id\":\"" + id + "\",\"description\":\"old copy\"}}", TestRunItems.class);

        read.showing(Optional.of(testCaseReading(id, "live")), Optional.empty(), Optional.empty());

        assertEquals(read.shownTestCase().getDescription(), "live", "an old copy in the file is not read");
    }
}
