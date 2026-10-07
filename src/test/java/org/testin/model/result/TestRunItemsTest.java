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

package org.testin.model.result;

import org.testin.model.TestCaseDto;
import org.testin.model.status.RunItemStatus;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class TestRunItemsTest {

    @Test
    public void shownTestCaseIsTheWiredTestCase() {
        final TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).build();
        final TestRunItems item = TestRunItems.builder().id(UUID.randomUUID()).build().showing(Optional.of(tc), Optional.empty(), Optional.empty());

        assertSame(item.shownTestCase(), tc);
        assertFalse(item.isRemoved(), "a row showing a live test case is not removed");
    }

    @Test
    public void showingNoTestCaseMarksTheRowRemovedAndNamesIt() {
        final UUID id = UUID.randomUUID();
        final TestRunItems item = TestRunItems.builder().id(id).build()
                .showing(Optional.of(TestCaseDto.builder().id(id).build()), Optional.empty(), Optional.empty())
                .showing(Optional.empty(), Optional.empty(), Optional.empty());

        assertTrue(item.isRemoved(), "the live test case and the removed mark are set by one call, so they cannot disagree");
        assertEquals(item.shownTestCase().getDescription(), TestCaseDto.deleted(id).getDescription());
    }

    @Test
    public void anUnwiredItemShowsAsARemovedTestCaseRatherThanThrowing() {
        final UUID id = UUID.randomUUID();
        final TestRunItems item = TestRunItems.builder().id(id).build();

        assertEquals(item.shownTestCase().getDescription(), TestCaseDto.deleted(id).getDescription());
        assertEquals(item.shownTestCase().getId(), id);
    }

    @Test
    public void anUnrunItemIsPending() {
        final TestRunItems item = TestRunItems.builder().id(UUID.randomUUID()).build();

        assertEquals(item.getStatus(), RunItemStatus.PENDING, "an unrun item defaults to PENDING");
    }

    @Test
    public void aFrameworkMeasurementIsRecordedWhereNoClockCounted() {
        final TestRunItems item = TestRunItems.builder().id(UUID.randomUUID()).build();
        item.setDuration(Duration.ofSeconds(3));

        item.recordDuration(Duration.ofMillis(84));

        assertEquals(item.getDuration(), Duration.ofMillis(84),
                "a test case no clock was counting takes the framework's own measure");
    }

    // Rule-EDITOR-PANEL-132
    @Test
    public void theClockStopsCountingOnceTheRunItemStatusIsIn() {
        final TestRunItems item = TestRunItems.builder().id(UUID.randomUUID()).build();
        item.recordDuration(Duration.ofMillis(10017));
        item.recordRunItemStatus(RunItemStatus.PASSED, "Muteb");

        item.recordClock(Duration.ofSeconds(24));

        assertEquals(item.getDuration(), Duration.ofMillis(10017),
                "a clock still ticking on a judged test case would save the tester's watching time as the method's");
    }

    @Test
    public void nothingMeasuredLeavesWhatIsThereAlone() {
        final TestRunItems item = TestRunItems.builder().id(UUID.randomUUID()).build();
        item.setDuration(Duration.ofSeconds(3));

        item.recordDuration(Duration.ZERO);

        assertEquals(item.getDuration(), Duration.ofSeconds(3),
                "a report carrying no duration must not erase one the clock counted");
    }

    @Test
    public void passingKeepsTheDurationThoughItClearsTheFailure() {
        final TestRunItems item = TestRunItems.builder().id(UUID.randomUUID()).build();
        item.recordDuration(Duration.ofMillis(84));
        item.setActualResult("expected [true] but found [false]");

        item.recordRunItemStatus(RunItemStatus.PASSED, "tester");

        assertEquals(item.getActualResult(), "");
        assertEquals(item.getDuration(), Duration.ofMillis(84), "a test case that passed still took time");
    }
}
