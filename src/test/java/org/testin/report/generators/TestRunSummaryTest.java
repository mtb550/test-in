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
    public void pendingCountsBothWaysOfNotHavingBeenRun() {
        // The regression: a completed run holds UNTESTED, not PENDING.
        final TestRunSummary summary = TestRunSummary.of(List.of(
                item(TestStatus.UNTESTED),
                item(TestStatus.UNTESTED),
                item(TestStatus.PENDING)));

        assertEquals(summary.pending(), 3, "UNTESTED is pending that has outlived the run");
    }

    @Test
    public void aCompletedRunDoesNotReportZeroOutstanding() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                item(TestStatus.PASSED),
                item(TestStatus.UNTESTED)));

        assertEquals(summary.pending(), 1);
        assertEquals(summary.passed(), 1);
        assertEquals(summary.passRate(), 50);
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
        assertEquals(summary.pending(), 1);
        assertEquals(summary.passRate(), 40);
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
