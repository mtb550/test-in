package org.testin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.ToLongFunction;

/**
 * The four verdicts a tester writes about after a run, and what a report calls
 * each of them.
 * <p>
 * The counts stay where they were - read from {@link TestRunSummary}, so the
 * heading over a paragraph and the table further down cannot disagree about how
 * many cases passed. What is new is the paragraph: the numbers say what happened
 * and only a tester can say why, and that sentence used to live in whatever
 * document they wrote by hand afterwards.
 * <p>
 * One declaration for all three report formats and the dialog that fills them
 * in. The color is a hex string rather than a format's own color type because
 * PDF, Word and HTML each have their own and all three already used these exact
 * values.
 */
@Getter
@AllArgsConstructor
public enum ResultAnalysis {

    PASSED(TestStatus.PASSED.getLabel(), "2E7D32", TestRunSummary::passed),
    FAILED(TestStatus.FAILED.getLabel(), "C0392B", TestRunSummary::failed),
    BLOCKED(TestStatus.BLOCKED.getLabel(), "B8860B", TestRunSummary::blocked),
    UNTESTED(TestStatus.UNTESTED.getLabel(), "595959", TestRunSummary::untested);

    private final @NotNull String label;
    private final @NotNull String hexColor;
    private final @NotNull ToLongFunction<TestRunSummary> count;

    /**
     * The heading over the paragraph: the verdict and how many cases carry it.
     */
    public @NotNull String heading(final @NotNull TestRunSummary summary) {
        return label + " (" + count.applyAsLong(summary) + ")";
    }

    /**
     * What the tester wrote about this verdict, and empty when they wrote
     * nothing - which is the whole of the rule the reports follow: a verdict
     * nobody commented on is not printed.
     */
    public @NotNull String writtenIn(final @NotNull Map<ResultAnalysis, String> analysis) {
        return analysis.getOrDefault(this, "").trim();
    }

    /**
     * Whether the tester wrote anything at all. A run nobody analysed prints no
     * Result Analysis section, rather than a heading over four empty ones.
     */
    public static boolean anyWrittenIn(final @NotNull Map<ResultAnalysis, String> analysis) {
        for (final ResultAnalysis section : values()) {
            if (!section.writtenIn(analysis).isEmpty()) return true;
        }

        return false;
    }
}
