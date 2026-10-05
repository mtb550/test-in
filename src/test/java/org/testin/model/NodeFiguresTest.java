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


import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.testin.model.result.TestRunItems;
import org.testin.model.result.TestRunSummary;
import org.testin.model.status.RunItemStatus;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

public class NodeFiguresTest {

    private static @NotNull TestRunItems item(final RunItemStatus status) {
        return TestRunItems.builder().id(UUID.randomUUID()).status(status).build();
    }

    // Rule-INTERNAL-049
    @Test
    public void aTestRunHoldsItsSummaryRatherThanACopyOfTheNumbers() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                item(RunItemStatus.PASSED),
                item(RunItemStatus.FAILED)));

        assertSame(NodeFigures.ofTestRun(summary).testRun(), summary,
                "seven fields copied out of the summary would make the popup a second "
                        + "implementation of how a test run went, which is what the summary exists to prevent");
    }

    @Test
    public void everyRunItemStatusReadsThroughThatSummary() {
        final NodeFigures figures = NodeFigures.ofTestRun(TestRunSummary.of(List.of(
                item(RunItemStatus.PASSED),
                item(RunItemStatus.PASSED),
                item(RunItemStatus.FAILED),
                item(RunItemStatus.BLOCKED),
                item(RunItemStatus.UNTESTED))));

        assertEquals(NodeCount.PASSED.of(figures), "2");
        assertEquals(NodeCount.FAILED.of(figures), "1");
        assertEquals(NodeCount.BLOCKED.of(figures), "1");
        assertEquals(NodeCount.UNTESTED.of(figures), "1");
        assertEquals(NodeCount.REMOVED.of(figures), "0");
        assertEquals(NodeCount.TOTAL.of(figures), "5");
        assertEquals(NodeCount.PASS_RATE.of(figures), "50%", "two of the four that ran passed");
    }

    // Rule-INTERNAL-052
    @Test
    public void aTestRunNobodyHasStartedSaysSoRatherThanReportingZeroPercent() {
        final NodeFigures untouched = NodeFigures.ofTestRun(TestRunSummary.of(List.of(
                item(RunItemStatus.PENDING),
                item(RunItemStatus.PENDING))));

        assertEquals(untouched.rateLabel(), "Not run");
    }

    @Test
    public void aTestRunWithARunItemStatusReportsItsRate() {
        final NodeFigures testRun = NodeFigures.ofTestRun(TestRunSummary.of(List.of(
                item(RunItemStatus.PASSED),
                item(RunItemStatus.PASSED),
                item(RunItemStatus.PASSED),
                item(RunItemStatus.FAILED))));

        assertEquals(testRun.rateLabel(), "75%");
    }

    @Test
    public void aContainerCountsWhatIsBeneathItAndHasNoTestRun() {
        final NodeFigures container = NodeFigures.ofChildren(9, 4, 2770, 2770, 2);

        assertEquals(container.testSets(), 9);
        assertEquals(container.packages(), 4);
        assertEquals(container.testCases(), 2770);
        assertEquals(container.testRuns(), 2);
        assertSame(container.testRun(), TestRunSummary.EMPTY, "a container has no test run to report");
    }

    @Test
    public void aCountReadsAsTheRowItWillBecome() {
        final NodeFigures figures = NodeFigures.ofChildren(9, 4, 2770, 2770, 2);

        assertEquals(NodeCount.TEST_CASES.of(figures), "2770");
        assertEquals(NodeCount.PASS_RATE.of(NodeFigures.NONE), "0%", "a rate carries its sign");
        assertEquals(NodeCount.TEST_RUNS.of(NodeFigures.NONE), "0",
                "an empty node answers zero; an absent row would read as 'not counted'");
    }

    // Rule-INTERNAL-065
    @Test
    public void aCountSaysWhatANewTestRunWouldTakeWhenThatIsFewer() {
        assertEquals(NodeCount.TEST_CASES.of(NodeFigures.ofChildren(9, 4, 40, 31, 2)), "40 (31 for a new test run)");
    }

    @Test
    public void aCountSaysItOnlyOnceWhenNothingIsRetired() {
        assertEquals(NodeCount.TEST_CASES.of(NodeFigures.ofChildren(9, 4, 40, 40, 2)), "40",
                "the same number twice is furniture, not an answer");
    }
}
