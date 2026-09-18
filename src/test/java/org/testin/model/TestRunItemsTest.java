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
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * The two contracts {@code tc} serves (#48).
 * <p>
 * A run item read straight out of the run JSON has no test case attached, and
 * one whose case has since been deleted never gets one - {@code FailedResultDialog}
 * opens for both and must cope. The editor, on the other hand, drops the items it
 * cannot resolve and assigns a case to every item it keeps, so nothing that
 * reaches a renderer is missing one.
 * <p>
 * Same field, two contracts. {@code testCase()} is for the first, {@code shownCase()}
 * for the second.
 */
public class TestRunItemsTest {

    @Test
    public void aFreshItemHasNoTestCaseUntilTheEditorWiresOne() {
        assertTrue(TestRunItems.builder().id(UUID.randomUUID()).build().testCase().isEmpty(),
                "the case is @JsonIgnore - deserializing a run never fills it");
    }

    @Test
    public void shownCaseIsTheWiredCase() {
        final TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).build();
        final TestRunItems item = TestRunItems.builder().id(UUID.randomUUID()).build().setTc(tc);

        assertSame(item.shownCase(), tc);
    }

    @Test
    public void anUnwiredItemShowsAsARemovedCaseRatherThanThrowing() {
        final UUID id = UUID.randomUUID();
        final TestRunItems item = TestRunItems.builder().id(id).build();

        // The placeholder a result whose case was deleted already draws, so a
        // renderer meeting an item nobody wired paints a row rather than failing.
        assertEquals(item.shownCase().getDescription(), TestCaseDto.deleted(id).getDescription());
        assertEquals(item.shownCase().getId(), id);
    }

    @Test
    public void testCaseStaysAvailableForTheCallersThatMustHandleItsAbsence() {
        final TestRunItems item = TestRunItems.builder().id(UUID.randomUUID()).build();

        // FailedResultDialog renders "No longer in the test set" from exactly this.
        assertTrue(item.testCase().isEmpty());
        assertEquals(item.getStatus(), org.testin.model.TestStatus.PENDING, "an unrun item defaults to PENDING");
    }

    @Test
    public void theFrameworksMeasurementOverridesWhatTheClockCounted() {
        final TestRunItems item = TestRunItems.builder().id(UUID.randomUUID()).build();
        item.setDuration(Duration.ofSeconds(3));

        item.recordDuration(Duration.ofMillis(84));

        assertEquals(item.getDuration(), Duration.ofMillis(84),
                "the clock times a tester reading a case; the framework times the method");
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

        item.recordVerdict(TestStatus.PASSED, "tester");

        assertEquals(item.getActualResult(), "");
        assertEquals(item.getDuration(), Duration.ofMillis(84), "a case that passed still took time");
    }
}
