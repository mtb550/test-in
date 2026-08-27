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
public record NodeFigures(long testSets, long packages, long testCases, long testRuns, @NotNull TestRunSummary run) {

    /**
     * What a node with nothing in it reports. Zero is an answer.
     */
    public static final @NotNull NodeFigures NONE = new NodeFigures(0, 0, 0, 0, TestRunSummary.EMPTY);

    public static @NotNull NodeFigures ofChildren(final long testSets, final long packages, final long testCases, final long testRuns) {
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

    /**
     * The sentence a removal confirmation shows, blank when the node holds
     * nothing - "and nothing else goes with it" is not worth a line.
     * <p>
     * On the counts rather than beside them, because there were two answers to
     * "what does this node hold" and they disagreed. The other one filtered the
     * index by path prefix, and a node's own path is a prefix of itself: so
     * removing a test set holding twelve cases read "Holds 1 test set, 12 test
     * cases and 0 test runs", and removing a run said it held one run. In the
     * one dialog whose whole job is to say what an unrecoverable delete takes
     * with it.
     * <p>
     * Packages are counted and deliberately not said. What a package holds is
     * already in these numbers, and the line is about what the tester loses.
     */
    public @NotNull String describe() {
        if (testSets == 0 && testCases == 0 && testRuns == 0) return "";

        return "Holds " + testSets + " test set" + plural(testSets)
                + ", " + testCases + " test case" + plural(testCases)
                + " and " + testRuns + " test run" + plural(testRuns);
    }

    private @NotNull String plural(final long count) {
        return count == 1 ? "" : "s";
    }
}
