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

import java.util.List;
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

    /**
     * An automated failure replaces what the last failure said and keeps the bug
     * it was reported as: the same run item failing again in the same run is
     * most often the same bug, and only a pass clears the link (#28, P26).
     */
    @Test
    public void anAutomatedFailureKeepsTheBugIssueLink() {
        final TestRunItems item = row().setBugIssueUrl("https://github.com/mtb550/product/issues/123");

        new Failure("expected [true] but found [false]", "at testProject.SPTestTest.check").recordOn(item);
        item.recordVerdict(TestStatus.FAILED, "tester");

        assertEquals(item.getBugIssueUrl(), "https://github.com/mtb550/product/issues/123");
    }

    @Test
    public void aReportedFailureClearsTheScreenshotsOfTheLastOne() {
        final TestRunItems item = row().setScreenshots(List.of("0123456789abcdef.png"));

        new Failure("expected [true] but found [false]", "at testProject.SPTestTest.check").recordOn(item);

        assertTrue(item.getScreenshots().isEmpty(), "a picture of the last failure would read as a picture of this one (#50)");
    }

    @Test
    public void nothingWentWrongLeavesTheScreenshotsAlone() {
        final TestRunItems item = row().setScreenshots(List.of("0123456789abcdef.png"));

        Failure.NONE.recordOn(item);

        assertEquals(item.getScreenshots().size(), 1, "a manual verdict must not erase them");
    }

    /**
     * What a reported failure clears and what it names are one list: what
     * happened, never the bug (#50).
     */
    @Test
    public void aReportedFailureNamesWhatHappenedAndNotTheBug() {
        final TestRunItems item = row().setActualResult("typed by hand").setScreenshots(List.of("0123456789abcdef.png")).setBugSeverity(BugSeverity.MAJOR);

        assertEquals(new Failure("boom", "").wouldClear(item), List.of("the actual result", "the screenshots"));
        assertEquals(Failure.NONE.wouldClear(item), List.of(), "a verdict given by hand clears nothing");
    }
}
