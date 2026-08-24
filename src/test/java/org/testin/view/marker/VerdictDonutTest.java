package org.testin.view.marker;

import org.testin.model.NodeCount;
import org.testin.model.NodeFigures;
import org.testin.model.NodeStatistics;
import org.testin.model.TestRunSummary;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * The arithmetic behind the ring (#82).
 * <p>
 * Two things have to hold whatever the run looks like: a verdict somebody
 * recorded is visible, and the ring still closes. They pull against each other
 * - a single failure in five hundred is three quarters of a degree, and giving
 * it enough of the ring to see costs the other arcs their share - so both are
 * checked on the same figures.
 */
public class VerdictDonutTest {

    private static final List<NodeCount> SLICES = NodeStatistics.VERDICTS.getSlices();

    private static NodeFigures run(final long passed, final long failed, final long blocked, final long untested, final long removed) {
        final long total = passed + failed + blocked + untested + removed;
        final long executed = passed + failed + blocked;

        return NodeFigures.ofRun(new TestRunSummary(total, passed, failed, blocked, untested, removed,
                executed == 0 ? 0 : (int) (passed * 100 / executed), ""));
    }

    private static double sum(final double[] sweeps) {
        return Arrays.stream(sweeps).sum();
    }

    @Test
    public void oneFailureInFiveHundredIsStillVisible() {
        final double[] sweeps = VerdictDonut.sweeps(SLICES, run(499, 1, 0, 0, 0));

        // Its true share is 0.72 degrees, which at this size is half a pixel.
        assertTrue(sweeps[1] > 2.0, "a failure somebody recorded has to be visible: " + sweeps[1]);
    }

    @Test
    public void theRingStillClosesWhenASliverWasEnlarged() {
        final double[] sweeps = VerdictDonut.sweeps(SLICES, run(497, 1, 1, 1, 0));

        assertEquals(sum(sweeps), 360.0, 0.0001, "the arcs must still add up to a circle");
    }

    @Test
    public void aVerdictWithNoCasesTakesNoneOfTheRing() {
        final double[] sweeps = VerdictDonut.sweeps(SLICES, run(420, 60, 40, 30, 0));

        assertEquals(sweeps[4], 0.0, 0.0, "nothing was removed, so nothing is drawn for it");
        assertEquals(sum(sweeps), 360.0, 0.0001);
    }

    @Test
    public void theWholeRingGoesToTheOnlyVerdictThereIs() {
        final double[] sweeps = VerdictDonut.sweeps(SLICES, run(550, 0, 0, 0, 0));

        assertEquals(sweeps[0], 360.0, 0.0001, "every case passed");
    }

    @Test
    public void aRunWithNothingInItDrawsNoArcsAndDividesByNothing() {
        final double[] sweeps = VerdictDonut.sweeps(SLICES, NodeFigures.NONE);

        assertEquals(sum(sweeps), 0.0, 0.0, "an empty run leaves the bare track showing");
    }

    @Test
    public void aNodeWithNoSlicesHasNoRing() {
        assertEquals(VerdictDonut.sweeps(NodeStatistics.CHILDREN.getSlices(),
                NodeFigures.ofChildren(9, 4, 2770, 2)).length, 0,
                "a container's counts are not parts of one whole, so nothing is drawn through them");
    }
}
