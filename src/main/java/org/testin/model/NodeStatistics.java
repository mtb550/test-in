package org.testin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.NodeCounter;

import java.util.List;

/**
 * The two ways a node reports itself: as a container of things, or as a run of
 * them.
 * <p>
 * Each constant carries how its figures are gathered and which of them the
 * chart draws, so a node type declares one word - {@code CHILDREN} or
 * {@code VERDICTS} - and everything else follows from it. The dialog reads the
 * declaration and never learns that either kind exists.
 * <p>
 * A statistics with no slices draws no chart, which is not a case anybody
 * tests: the chart is as tall as the slices it was given, and no slices is no
 * height. That is the same rule the details rows follow, where a blank value is
 * simply not a row.
 */
@Getter
@AllArgsConstructor
public enum NodeStatistics {

    /**
     * A container: counted from what lies beneath it. No chart - four counts
     * of unrelated things are a list, and a ring drawn through them would
     * claim they were parts of one whole.
     */
    CHILDREN(NodeCounter::childCounts, List.of()),

    /**
     * A test run: counted from the verdicts it recorded. Its five verdicts do
     * divide one whole - every case in the run is in exactly one of them - so
     * they are what the ring is drawn from.
     */
    VERDICTS(NodeCounter::runVerdicts, List.of(
            NodeCount.PASSED, NodeCount.FAILED, NodeCount.BLOCKED, NodeCount.UNTESTED, NodeCount.REMOVED));

    private final @NotNull FiguresGatherer gather;

    /**
     * The counts the chart draws as arcs, in the order they are drawn, and
     * empty for a node whose numbers are not parts of one whole.
     */
    private final @NotNull List<NodeCount> slices;
}
