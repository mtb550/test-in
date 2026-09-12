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

import org.testng.annotations.Test;

import java.util.UUID;

import static org.testng.Assert.*;

/**
 * What a run row keeps when a test framework reports one of its cases.
 * <p>
 * The rule worth a test is the empty one. Every verdict passes a failure now,
 * including the ones a tester gives by hand, so a {@link Failure#NONE} that
 * wrote itself in would quietly wipe what they had typed into
 * {@code FailedResultDialog} - and it would do it on the happy path, on the way
 * to a green Passed.
 */
public class FailureTest {

    private static TestRunItems row() {
        return TestRunItems.builder().id(UUID.randomUUID()).build();
    }

    @Test
    public void aReportedFailureFillsTheTwoFieldsATesterWouldHaveTyped() {
        final TestRunItems item = row();

        new Failure("expected [true] but found [false]", "at testProject.SPTestTest.check(SPTestTest.java:42)").recordOn(item);

        assertEquals(item.getActualResult(), "expected [true] but found [false]");
        assertTrue(item.getStacktrace().contains("SPTestTest.java:42"));
    }

    @Test
    public void nothingWentWrongLeavesWhatTheTesterTypedAlone() {
        final TestRunItems item = row();
        item.setActualResult("the dialog never opened");
        item.setStacktrace("pasted by hand");

        Failure.NONE.recordOn(item);

        assertEquals(item.getActualResult(), "the dialog never opened", "a manual verdict must not erase this");
        assertEquals(item.getStacktrace(), "pasted by hand");
    }

    @Test
    public void aFailureWithNoStacktraceDoesNotInheritTheLastRunsOne() {
        final TestRunItems item = row();
        item.setStacktrace("at testProject.SPTestTest.check(SPTestTest.java:42)");

        new Failure("Skipped/Terminated", "").recordOn(item);

        assertEquals(item.getStacktrace(), "",
                "the row describes this run, and an older stacktrace would read as the explanation of this one");
    }

    @Test
    public void passingAfterwardsClearsWhatTheFailureRecorded() {
        final TestRunItems item = row();

        // The order RunStatusService.executeManual uses: record, then judge.
        new Failure("expected [true] but found [false]", "at testProject.SPTestTest.check").recordOn(item);
        item.recordVerdict(TestStatus.PASSED, "tester");

        assertEquals(item.getActualResult(), "", "a case that passed has nothing to explain");
        assertEquals(item.getStacktrace(), "");
    }

    @Test
    public void failingKeepsIt() {
        final TestRunItems item = row();

        new Failure("expected [true] but found [false]", "at testProject.SPTestTest.check").recordOn(item);
        item.recordVerdict(TestStatus.FAILED, "tester");

        assertEquals(item.getActualResult(), "expected [true] but found [false]");
        assertEquals(item.getExecutedBy(), "tester");
    }
}
