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

import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.util.Mapper;
import org.testin.util.RealMapper;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * A run row keeps the test case its verdict was given against (#306, piece 1).
 * <p>
 * Rule-EDITOR-PANEL-238 to 241: an execution records the case as it is at that
 * moment, a correction keeps what was judged, running a row again takes the case
 * as it is now, and a case deleted afterwards keeps its full text in the run.
 */
public class CaseAsJudgedTest {
    private static final Mapper MAPPER = RealMapper.build();

    private static TestCaseDto caseReading(final UUID id, final String description) {
        return TestCaseDto.builder().id(id).description(description).build();
    }

    private static TestCaseDto copyOf(final TestCaseDto tc) {
        return MAPPER.readValue(MAPPER.writeValueAsString(tc), TestCaseDto.class);
    }

    @Test
    public void aVerdictKeepsTheCaseItWasGivenAgainst() {
        final UUID id = UUID.randomUUID();
        final TestCaseDto live = caseReading(id, "before");
        final TestRunItems item = TestRunItems.builder().id(id).build().showing(Optional.of(live));

        item.recordVerdict(TestStatus.PASSED, "tester", copyOf(live));
        live.setDescription("after");
        item.showing(Optional.of(live));

        assertEquals(item.shownCase().getDescription(), "before", "the row shows what was executed");
        assertEquals(item.liveCase().getDescription(), "after", "actions still reach the case as it is now");
    }

    @Test
    public void aCorrectionKeepsTheJudgedCase() {
        final UUID id = UUID.randomUUID();
        final TestRunItems item = TestRunItems.builder().id(id).build();
        item.recordVerdict(TestStatus.PASSED, "tester", caseReading(id, "before"));

        item.correctVerdict(TestStatus.FAILED, "tester", caseReading(id, "after"));

        assertEquals(item.getStatus(), TestStatus.FAILED);
        assertEquals(item.shownCase().getDescription(), "before", "only the verdict, who and when change");
    }

    @Test
    public void aCorrectionOnARowNeverJudgedTakesTheCaseAsItIsNow() {
        final UUID id = UUID.randomUUID();
        final TestRunItems item = TestRunItems.builder().id(id).build();

        item.correctVerdict(TestStatus.BLOCKED, "tester", caseReading(id, "now"));

        assertEquals(item.shownCase().getDescription(), "now");
    }

    @Test
    public void runningAJudgedRowAgainTakesTheCaseAsItIsNow() {
        final UUID id = UUID.randomUUID();
        final TestRunItems item = TestRunItems.builder().id(id).build();
        item.recordVerdict(TestStatus.FAILED, "tester", caseReading(id, "before"));

        item.recordVerdict(TestStatus.PASSED, "tester", caseReading(id, "after"));

        assertEquals(item.shownCase().getDescription(), "after");
    }

    @Test
    public void aCaseDeletedAfterItsVerdictKeepsItsFullTextAndItsVerdict() {
        final UUID id = UUID.randomUUID();
        final TestRunItems item = TestRunItems.builder().id(id).build();
        item.recordVerdict(TestStatus.PASSED, "tester", caseReading(id, "before"));

        item.showing(Optional.empty());

        assertTrue(item.isRemoved(), "nothing may act on the row of a case that no longer exists");
        assertEquals(item.shownStatus(), TestStatus.PASSED, "the result is shown whatever happened to the test case");
        assertEquals(item.shownCase().getDescription(), "before", "not the placeholder");
    }

    @Test
    public void theJudgedCaseTakesItsTestSetFromTheLiveCase() {
        final UUID id = UUID.randomUUID();
        final TestSetDirectoryDto set = new TestSetDirectoryDto();
        final TestRunItems item = TestRunItems.builder().id(id).build();
        item.recordVerdict(TestStatus.PASSED, "tester", caseReading(id, "before"));

        item.showing(Optional.of(TestCaseDto.builder().id(id).parent(set).build()));

        assertSame(item.shownCase().getParent(), set, "where the case sits is the live case's, so navigation still works");
    }

    @Test
    public void aResultNotJudgedYetIsWrittenWithoutATestCase() {
        final String written = MAPPER.writeValueAsString(TestRunItems.builder().id(UUID.randomUUID()).build());

        assertFalse(written.contains("testCase"), "a pending result carries no testCase key: " + written);
    }

    @Test
    public void aJudgedResultIsWrittenWithItsTestCaseAndReadBack() {
        final UUID id = UUID.randomUUID();
        final TestRunItems item = TestRunItems.builder().id(id).build();
        item.recordVerdict(TestStatus.PASSED, "tester", caseReading(id, "as executed"));

        final String written = MAPPER.writeValueAsString(item);
        final TestRunItems read = MAPPER.readValue(written, TestRunItems.class);

        assertTrue(written.contains("\"testCase\""), written);
        assertEquals(read.shownCase().getDescription(), "as executed");
    }

    @Test
    public void aResultWrittenBeforeThisChangeShowsTheLiveCase() {
        final UUID id = UUID.randomUUID();
        final TestRunItems read = MAPPER.readValue("{\"id\":\"" + id + "\",\"status\":\"PASSED\"}", TestRunItems.class);

        read.showing(Optional.of(caseReading(id, "live")));

        assertEquals(read.shownCase().getDescription(), "live", "an old result reads as not judged against anything");
    }
}
