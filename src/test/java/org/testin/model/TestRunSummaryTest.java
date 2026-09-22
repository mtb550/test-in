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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestRunSummaryTest {

    private static TestRunItems item(final TestStatus status) {
        return TestRunItems.builder().id(UUID.randomUUID()).status(status).build();
    }

    private static TestRunItems ranBy(final String tester) {
        return TestRunItems.builder().id(UUID.randomUUID()).status(TestStatus.PASSED).executedBy(tester).build();
    }

    private static int rateOf(final int passed, final int failed) {
        final List<TestRunItems> results = new ArrayList<>();
        for (int i = 0; i < passed; i++) results.add(item(TestStatus.PASSED));
        for (int i = 0; i < failed; i++) results.add(item(TestStatus.FAILED));

        return TestRunSummary.of(results).passRate();
    }

    @Test
    public void thePassRateRoundsRatherThanTruncating() {
        assertEquals(rateOf(2, 1), 67, "two passed of three is 66.67%, which reads 67");
        assertEquals(rateOf(1, 2), 33, "one passed of three is 33.33%, which reads 33");
        assertEquals(rateOf(1, 1), 50, "one passed of two is exactly half");
        assertEquals(rateOf(1, 0), 100, "everything that ran passed");
        assertEquals(rateOf(0, 1), 0, "nothing that ran passed");
    }

    @Test
    public void aDeletedTestCaseCountsUnderItsVerdictOnceJudgedAndUnderRemovedOtherwise() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                item(TestStatus.PASSED),
                item(TestStatus.FAILED),
                TestRunItems.builder().id(UUID.randomUUID()).status(TestStatus.PASSED).removed(true).build(),
                TestRunItems.builder().id(UUID.randomUUID()).status(TestStatus.PENDING).removed(true).build()));

        assertEquals(summary.passed(), 2, "the deleted case's Passed is still a result");
        assertEquals(summary.removed(), 1, "only the row never judged is Removed");
        assertEquals(summary.passRate(), 67, "two passed of the three judged");
        assertEquals(summary.total(), 4, "the total still counts every row");
    }

    @Test
    public void untestedCountsBothWaysOfNotHavingBeenRun() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                item(TestStatus.UNTESTED),
                item(TestStatus.UNTESTED),
                item(TestStatus.PENDING)));

        assertEquals(summary.untested(), 3, "PENDING is untested that the run has not reached yet");
    }

    @Test
    public void aCompletedRunDoesNotReportZeroOutstanding() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                item(TestStatus.PASSED),
                item(TestStatus.UNTESTED)));

        assertEquals(summary.untested(), 1);
        assertEquals(summary.passed(), 1);
        assertEquals(summary.passRate(), 100, "one case ran and it passed; the untested one is not a failure");
    }

    @Test
    public void eachStatusIsCountedUnderItsOwnName() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                item(TestStatus.PASSED),
                item(TestStatus.PASSED),
                item(TestStatus.FAILED),
                item(TestStatus.BLOCKED),
                item(TestStatus.PENDING)));

        assertEquals(summary.total(), 5);
        assertEquals(summary.passed(), 2);
        assertEquals(summary.failed(), 1);
        assertEquals(summary.blocked(), 1);
        assertEquals(summary.untested(), 1);
        assertEquals(summary.executed(), 4, "passed, failed and blocked were run; the pending one was not");
        assertEquals(summary.passRate(), 50, "2 of the 4 that ran");
    }

    @Test
    public void theFiguresUnderTheTotalAddUpToIt() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                item(TestStatus.PASSED),
                item(TestStatus.FAILED),
                item(TestStatus.BLOCKED),
                item(TestStatus.UNTESTED),
                item(TestStatus.PENDING),
                item(TestStatus.REMOVED),
                item(TestStatus.REMOVED)));

        assertEquals(summary.total(), 7);
        assertEquals(summary.removed(), 2);
        assertEquals(summary.passed() + summary.failed() + summary.blocked()
                + summary.untested() + summary.removed(), summary.total());
    }

    @Test
    public void theRemovedTileShowsOnlyWhenThereIsSomethingToShow() {
        final TestRunSummary ordinary = TestRunSummary.of(List.of(
                item(TestStatus.PASSED),
                item(TestStatus.UNTESTED)));

        final TestRunSummary withRemoved = TestRunSummary.of(List.of(
                item(TestStatus.PASSED),
                item(TestStatus.REMOVED)));

        assertFalse(ordinary.hasRemoved(), "an ordinary run prints six figures, not a seventh reading zero");
        assertTrue(withRemoved.hasRemoved());
    }

    @Test
    public void aRemovedTestCaseIsNeitherUntestedNorExecuted() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                item(TestStatus.PASSED),
                item(TestStatus.REMOVED)));

        assertEquals(summary.untested(), 0);
        assertEquals(summary.executed(), 1);
        assertEquals(summary.passRate(), 100, "the removed case is not a case that failed to pass");
    }

    @Test
    public void anEmptyRunHasNoPassRateRatherThanDividingByZero() {
        final TestRunSummary summary = TestRunSummary.of(List.of());

        assertEquals(summary.total(), 0);
        assertEquals(summary.passRate(), 0);
    }

    @Test
    public void untestedTestCasesDoNotDragThePassRateDown() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                item(TestStatus.PASSED),
                item(TestStatus.PASSED),
                item(TestStatus.UNTESTED),
                item(TestStatus.UNTESTED),
                item(TestStatus.PENDING)));

        assertEquals(summary.total(), 5);
        assertEquals(summary.executed(), 2);
        assertEquals(summary.passRate(), 100);
    }

    @Test
    public void blockedCountsAgainstThePassRate() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                item(TestStatus.PASSED),
                item(TestStatus.BLOCKED)));

        assertEquals(summary.executed(), 2);
        assertEquals(summary.passRate(), 50);
    }

    @Test
    public void aRunNobodyStartedHasNoRateRatherThanZeroPercentOfNothing() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                item(TestStatus.PENDING),
                item(TestStatus.PENDING)));

        assertEquals(summary.executed(), 0);
        assertEquals(summary.passRate(), 0);
    }

    @Test
    public void executedByNamesEveryoneWhoRecordedAVerdict() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                ranBy("Omar"), ranBy("Sara"), ranBy("Omar")));

        assertEquals(summary.executedBy(), "Omar, Sara", "each tester once, in the order they first appear");
    }

    @Test
    public void executedByIgnoresTestCasesNobodyRan() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                ranBy("Omar"), item(TestStatus.UNTESTED), ranBy("   ")));

        assertEquals(summary.executedBy(), "Omar");
    }

    @Test
    public void executedByIsEmptyRatherThanNullOnARunNobodyTouched() {
        assertEquals(TestRunSummary.of(List.of(item(TestStatus.PENDING))).executedBy(), "");
        assertEquals(TestRunSummary.of(List.of()).executedBy(), "");
    }
}
