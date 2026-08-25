package org.testin.report.generators;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunSummary;
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
            "F2685A", TestRunSummary::failed, true, TestStatus.FAILED),

    PASSED("Passed Test Cases",
            "The following %s cases passed validation and behaved as expected across all verification points.",
            "4FBF60", TestRunSummary::passed, false, TestStatus.PASSED),

    BLOCKED("Blocked Test Cases",
            "The following %s cases were attempted but could not complete, typically because of an environment or data dependency.",
            "F5B940", TestRunSummary::blocked, false, TestStatus.BLOCKED),

    /**
     * Both ways of not having been run. A case is PENDING while its run is open
     * and UNTESTED once the run finishes without reaching it, which is one fact
     * at two moments.
     * <p>
     * So one table. Two would mean a completed run printing a heading and a
     * count above an empty one.
     */
    UNTESTED("Untested Test Cases",
            "The following %s cases were not executed in this cycle and carry forward to the next run.",
            "96A1B0", TestRunSummary::untested, false, TestStatus.PENDING, TestStatus.UNTESTED),

    /**
     * The test case was deleted after this run recorded it. The run keeps the
     * row - it executed that case once, and deleting it later does not undo
     * that - so the report says so rather than dropping a row and leaving its
     * own total unexplained.
     */
    REMOVED("Removed Test Cases",
            "The following %s cases were removed from the test suite after this run recorded them.",
            "96A1B0", TestRunSummary::removed, false, TestStatus.REMOVED);

    private final @NotNull String title;
    private final @NotNull String descriptionFmt;
    /**
     * The colour of this table's header row, as hex.
     * <p>
     * Here because all three formats need it and two of them had already
     * written it out as a switch of their own - the same five cases, twice, in
     * files that cannot see each other. HTML had neither switch nor colour, so
     * its tables were headed a flat pale blue while the PDF and the Word file
     * headed them red, green, amber and grey.
     * <p>
     * Brighter than the text colors of the same names elsewhere in the reports.
     * These are fills carrying white text at a glance, not sentences to be read
     * closely, and the darker mix made a row of them look like one grey band.
     */
    @Getter
    private final @NotNull String hexColor;
    private final @NotNull ToLongFunction<TestRunSummary> count;
    /**
     * True for the tables that carry priority, severity and the actual result:
     * the columns only say anything about a case that failed.
     */
    @Getter
    private final boolean withFailureDetail;
    private final @NotNull Set<TestStatus> statuses;

    ReportSection(final @NotNull String title, final @NotNull String descriptionFmt, final @NotNull String hexColor, final @NotNull ToLongFunction<TestRunSummary> count, final boolean withFailureDetail, final @NotNull TestStatus... statuses) {
        this.title = title;
        this.descriptionFmt = descriptionFmt;
        this.hexColor = hexColor;
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
     * The sentence under the heading. Takes the count already rendered, because
     * HTML emboldens the number and the other two do not.
     */
    public @NotNull String description(final @NotNull String renderedCount) {
        return String.format(descriptionFmt, renderedCount);
    }

    /**
     * The text color that reads on this header fill, as hex.
     * <p>
     * Worked out from the fill rather than written beside it. White text was
     * assumed on all five, and the amber header had drifted to 1.8:1 against
     * white - unreadable at any size - without anything failing. Brightening
     * them made that worse on three more, so the question is answered from the
     * color itself and cannot fall out of step with it again.
     */
    public @NotNull String textHex() {
        return contrast(hexColor, "FFFFFF") >= contrast(hexColor, "14171A") ? "FFFFFF" : "14171A";
    }

    /**
     * How far apart two colors are to the eye, by the WCAG ratio. Only the
     * comparison matters here, never the number.
     */
    private static double contrast(final @NotNull String one, final @NotNull String other) {
        final double first = luminance(one);
        final double second = luminance(other);

        return (Math.max(first, second) + 0.05) / (Math.min(first, second) + 0.05);
    }

    private static double luminance(final @NotNull String hex) {
        return 0.2126 * channel(hex, 0) + 0.7152 * channel(hex, 2) + 0.0722 * channel(hex, 4);
    }

    private static double channel(final @NotNull String hex, final int at) {
        final double raw = Integer.parseInt(hex.substring(at, at + 2), 16) / 255.0;

        return raw <= 0.03928 ? raw / 12.92 : Math.pow((raw + 0.055) / 1.055, 2.4);
    }

    public boolean matches(final @NotNull TestRunItems item) {
        return statuses.contains(item.getStatus());
    }
}
