package org.testin.report.generators;

import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.ToLongFunction;

/**
 * The per-status case tables a report prints after its summary, in the order
 * they appear. Failures first: a reader who stops after one table should have
 * read the one that needs action.
 * <p>
 * Shared because the three formats each held their own copy of these four
 * definitions, and copies drift. They did: HTML's outstanding-work table
 * filtered on PENDING alone while PDF and Word folded BLOCKED in with it, so one
 * run produced three reports that disagreed about what had been run and printed
 * the same figure under two different headings. Heading, blurb and — the part
 * that broke — which statuses belong to which table are now stated once.
 * <p>
 * Rendering is deliberately not shared: iText, Word and HTML have nothing in
 * common to abstract over, and pretending otherwise would cost more than the
 * duplication it removed.
 */
enum ReportSection {

    FAILED("Failed Test Cases",
            "The following %s cases failed and require remediation.",
            TestRunSummary::failed, true, TestStatus.FAILED),

    PASSED("Passed Test Cases",
            "The following %s cases passed validation and behaved as expected across all verification points.",
            TestRunSummary::passed, false, TestStatus.PASSED),

    BLOCKED("Blocked Test Cases",
            "The following %s cases were attempted but could not complete, typically because of an environment or data dependency.",
            TestRunSummary::blocked, false, TestStatus.BLOCKED),

    /**
     * Both ways of not having been run. A case is PENDING while its run is open
     * and UNTESTED once the run finishes without reaching it, which is one fact
     * at two moments — so one table, or a completed run would print a heading
     * and a count above an empty table.
     */
    UNTESTED("Untested Test Cases",
            "The following %s cases were not executed in this cycle and carry forward to the next run.",
            TestRunSummary::untested, false, TestStatus.PENDING, TestStatus.UNTESTED),

    /**
     * The test case was deleted after this run recorded it. The run keeps the
     * row - it executed that case once, and deleting it later does not undo
     * that - so the report says so rather than dropping a row and leaving its
     * own total unexplained.
     */
    REMOVED("Removed Test Cases",
            "The following %s cases were removed from the test suite after this run recorded them.",
            TestRunSummary::removed, false, TestStatus.REMOVED);

    private final @NotNull String title;
    private final @NotNull String descriptionFmt;
    private final @NotNull ToLongFunction<TestRunSummary> count;
    private final boolean withFailureDetail;
    private final @NotNull Set<TestStatus> statuses;

    ReportSection(final @NotNull String title, final @NotNull String descriptionFmt,
                  final @NotNull ToLongFunction<TestRunSummary> count, final boolean withFailureDetail,
                  final @NotNull TestStatus... statuses) {
        this.title = title;
        this.descriptionFmt = descriptionFmt;
        this.count = count;
        this.withFailureDetail = withFailureDetail;
        this.statuses = EnumSet.copyOf(Arrays.asList(statuses));
    }

    public @NotNull String getTitle() {
        return title;
    }

    /**
     * How many cases this table will hold, read from the summary rather than
     * counted again, so the heading and the rows below it cannot disagree.
     */
    public long count(final @NotNull TestRunSummary summary) {
        return count.applyAsLong(summary);
    }

    /**
     * True for the tables that carry priority, severity and the actual result:
     * the columns only say anything about a case that failed.
     */
    public boolean isWithFailureDetail() {
        return withFailureDetail;
    }

    /**
     * The sentence under the heading. Takes the count already rendered, because
     * HTML emboldens the number and the other two do not.
     */
    public @NotNull String description(final @NotNull String renderedCount) {
        return String.format(descriptionFmt, renderedCount);
    }

    public boolean matches(final @NotNull TestRunItems item) {
        return statuses.contains(item.getStatus());
    }
}
