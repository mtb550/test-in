package org.testin.report.generators;

import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

/**
 * The headline counts every report format shares (#48).
 * <p>
 * These exist because the three generators counted for themselves and drifted:
 * PDF and Word treated pending as PENDING + UNTESTED, HTML counted PENDING
 * alone. Completing a run turns every PENDING case into UNTESTED, so on exactly
 * the runs a reader cares about - the finished ones - the HTML report claimed
 * nothing was outstanding while the other two reported the truth.
 */
public class TestRunSummaryTest {

    private static TestRunItems item(final TestStatus status) {
        return TestRunItems.builder().id(UUID.randomUUID()).status(status).build();
    }

    private static TestRunItems ranBy(final String tester) {
        return TestRunItems.builder().id(UUID.randomUUID()).status(TestStatus.PASSED).executedBy(tester).build();
    }

    @Test
    public void untestedCountsBothWaysOfNotHavingBeenRun() {
        // The regression: a completed run holds UNTESTED, not PENDING.
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
    public void anEmptyRunHasNoPassRateRatherThanDividingByZero() {
        final TestRunSummary summary = TestRunSummary.of(List.of());

        assertEquals(summary.total(), 0);
        assertEquals(summary.passRate(), 0);
    }

    @Test
    public void passRateTruncatesRatherThanRounding() {
        // 1 of 3 is 33.33; the reports have always shown 33.
        final TestRunSummary summary = TestRunSummary.of(List.of(
                item(TestStatus.PASSED),
                item(TestStatus.FAILED),
                item(TestStatus.FAILED)));

        assertEquals(summary.passRate(), 33);
    }

    // ------------------------------------------------------- what the rate is of

    /**
     * The rate measures the cases that were run, not the size of the run. It used
     * to divide by every case, so building a hundred and running ten reported 10%
     * even when all ten passed - a number that described how much work was left
     * rather than how the tests did.
     */
    @Test
    public void untestedCasesDoNotDragThePassRateDown() {
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

    /**
     * Blocked counts as run: it was attempted and something stopped it, which is
     * a result the rate should reflect.
     */
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

    // ------------------------------------------------------------ executed by

    /**
     * Who ran the tests, not who printed the report. The HTML generator used to
     * put the current tester's name here while PDF and Word read the run — so a
     * lead exporting someone else's cycle was credited with executing it.
     */
    @Test
    public void executedByNamesEveryoneWhoRecordedAVerdict() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                ranBy("Omar"), ranBy("Sara"), ranBy("Omar")));

        assertEquals(summary.executedBy(), "Omar, Sara", "each tester once, in the order they first appear");
    }

    @Test
    public void executedByIgnoresCasesNobodyRan() {
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
