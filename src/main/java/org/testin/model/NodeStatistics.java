package org.testin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The two ways a node reports itself: as a container of things, or as a run of
 * them.
 * <p>
 * A node kind names one of these words - see {@link DirectoryType} - and what
 * its Details draws follows from it. The dialog reads the declaration and never
 * learns that either kind exists.
 * <p>
 * <b>The word, not the counting.</b> Which method arrives at the numbers is
 * {@code NodeCounter}'s to say, because counting a node means walking the
 * index; this enum carried a reference to it until #111, which is how the
 * vocabulary package came to import the indexer. The two enums are named the
 * same, and a test says so.
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
    CHILDREN(List.of()),

    /**
     * A test run: counted from the verdicts it recorded. Its five verdicts do
     * divide one whole - every case in the run is in exactly one of them - so
     * they are what the ring is drawn from.
     */
    VERDICTS(List.of(
            NodeCount.PASSED, NodeCount.FAILED, NodeCount.BLOCKED, NodeCount.UNTESTED, NodeCount.REMOVED));

    /**
     * The counts the chart draws as arcs, in the order they are drawn, and
     * empty for a node whose numbers are not parts of one whole.
     */
    private final @NotNull List<NodeCount> slices;
}
