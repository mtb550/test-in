package org.testin.model;

import org.jetbrains.annotations.NotNull;

/**
 * Every number a tree node can report about itself, gathered in one pass.
 * <p>
 * One record rather than one per family, so a caller reads the field it wants
 * without asking what kind of node produced it. A container fills the four
 * counts and leaves the run empty; a test run does the opposite. Which of those
 * two happens is the node type's declaration, not a test anybody makes - see
 * {@link NodeStatistics}.
 * <p>
 * The run is held, not copied. Carrying its seven numbers as seven fields of
 * this record would have made the popup the second implementation of how a run
 * went - which is the exact thing {@link TestRunSummary} exists to stop, after
 * three report formats each counted for themselves and drifted apart.
 *
 * @param testSets  test sets beneath the node, at any depth
 * @param packages  packages beneath the node, at any depth
 * @param testCases test cases beneath the node, at any depth
 * @param testRuns  test runs beneath the node, at any depth
 * @param run       how the run went, and {@link TestRunSummary#EMPTY} for every
 *                  node that is not one
 */
public record NodeFigures(long testSets, long packages, long testCases, long testRuns,
                          @NotNull TestRunSummary run) {

    /**
     * What a node with nothing in it reports. Zero is an answer.
     */
    public static final @NotNull NodeFigures NONE = new NodeFigures(0, 0, 0, 0, TestRunSummary.EMPTY);

    public static @NotNull NodeFigures ofChildren(final long testSets, final long packages,
                                                  final long testCases, final long testRuns) {
        return new NodeFigures(testSets, packages, testCases, testRuns, TestRunSummary.EMPTY);
    }

    public static @NotNull NodeFigures ofRun(final @NotNull TestRunSummary summary) {
        return new NodeFigures(0, 0, 0, 0, summary);
    }

    /**
     * What the chart's hole reads.
     * <p>
     * A run nobody has given a verdict in has no rate to report, and "0%" there
     * would read as "every case failed" rather than "nothing has been run yet".
     * So it says which it is - and says it here, once, so the chart draws the
     * label it is handed instead of testing the run for itself.
     * <p>
     * Which cases count as run is {@link TestRunSummary#executed()}'s answer,
     * not a second one written here.
     */
    public @NotNull String rateLabel() {
        return run.executed() == 0 ? "Not run" : run.passRate() + "%";
    }
}
