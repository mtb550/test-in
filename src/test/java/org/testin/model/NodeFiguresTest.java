package org.testin.model;

import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

/**
 * What a node reports about itself (#82).
 * <p>
 * The two that matter are here: that a run's figures are the report's figures
 * rather than a second count of the same results, and that a run nobody has
 * given a verdict in says so instead of claiming a rate of zero - which reads
 * as "every case failed" on exactly the runs where nothing has been tried.
 */
public class NodeFiguresTest {

    private static TestRunItems item(final TestStatus status) {
        return TestRunItems.builder().id(UUID.randomUUID()).status(status).build();
    }

    @Test
    public void aRunHoldsItsSummaryRatherThanACopyOfTheNumbers() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                item(TestStatus.PASSED),
                item(TestStatus.FAILED)));

        assertSame(NodeFigures.ofRun(summary).run(), summary,
                "seven fields copied out of the summary would make the popup a second "
                        + "implementation of how a run went, which is what the summary exists to prevent");
    }

    @Test
    public void everyVerdictReadsThroughThatSummary() {
        final NodeFigures figures = NodeFigures.ofRun(TestRunSummary.of(List.of(
                item(TestStatus.PASSED),
                item(TestStatus.PASSED),
                item(TestStatus.FAILED),
                item(TestStatus.BLOCKED),
                item(TestStatus.UNTESTED))));

        assertEquals(NodeCount.PASSED.of(figures), "2");
        assertEquals(NodeCount.FAILED.of(figures), "1");
        assertEquals(NodeCount.BLOCKED.of(figures), "1");
        assertEquals(NodeCount.UNTESTED.of(figures), "1");
        assertEquals(NodeCount.REMOVED.of(figures), "0");
        assertEquals(NodeCount.TOTAL.of(figures), "5");
        assertEquals(NodeCount.PASS_RATE.of(figures), "50%", "two of the four that ran passed");
    }

    @Test
    public void aRunNobodyHasStartedSaysSoRatherThanReportingZeroPercent() {
        final NodeFigures untouched = NodeFigures.ofRun(TestRunSummary.of(List.of(
                item(TestStatus.PENDING),
                item(TestStatus.PENDING))));

        assertEquals(untouched.rateLabel(), "Not run");
    }

    @Test
    public void aRunWithAVerdictReportsItsRate() {
        final NodeFigures run = NodeFigures.ofRun(TestRunSummary.of(List.of(
                item(TestStatus.PASSED),
                item(TestStatus.PASSED),
                item(TestStatus.PASSED),
                item(TestStatus.FAILED))));

        assertEquals(run.rateLabel(), "75%");
    }

    @Test
    public void aContainerCountsWhatIsBeneathItAndHasNoRun() {
        final NodeFigures container = NodeFigures.ofChildren(9, 4, 2770, 2);

        assertEquals(container.testSets(), 9);
        assertEquals(container.packages(), 4);
        assertEquals(container.testCases(), 2770);
        assertEquals(container.testRuns(), 2);
        assertSame(container.run(), TestRunSummary.EMPTY, "a container has no run to report");
    }

    @Test
    public void aCountReadsAsTheRowItWillBecome() {
        final NodeFigures figures = NodeFigures.ofChildren(9, 4, 2770, 2);

        assertEquals(NodeCount.TEST_CASES.of(figures), "2770");
        assertEquals(NodeCount.PASS_RATE.of(NodeFigures.NONE), "0%", "a rate carries its sign");
        assertEquals(NodeCount.TEST_RUNS.of(NodeFigures.NONE), "0",
                "an empty node answers zero; an absent row would read as 'not counted'");
    }
}
