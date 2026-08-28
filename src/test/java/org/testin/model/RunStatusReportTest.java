package org.testin.model;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * What a runner's report means for the run editor that claimed the case.
 * <p>
 * The editor holds a claim on every case it launched, and that claim is what
 * makes a verdict land in the run the tester started rather than in some other
 * run holding the same case. It is released on the first report that says the
 * case is no longer going - so the question of which reports those are decides
 * whether a claim can outlive its execution, and a claim that does hands its run
 * the next verdict that case earns anywhere else.
 */
public class RunStatusReportTest {

    /**
     * One status means "still going", and the editor keeps its claim for exactly
     * that one.
     */
    @Test
    public void onlyRunningSaysTheCaseIsStillGoing() {
        assertTrue(RunStatus.RUNNING.stillGoing());

        assertEquals(java.util.Arrays.stream(RunStatus.values()).filter(RunStatus::stillGoing).count(), 1);
    }

    /**
     * A verdict is the end of the case, so a report carrying one always releases
     * the claim. The pairing is what the editor relies on - it records the
     * verdict and lets go in the same breath.
     */
    @Test
    public void everyVerdictEndsTheExecutionItReportsOn() {
        for (final RunStatus status : RunStatus.values()) {
            if (status.getVerdict().isEmpty()) continue;

            assertFalse(status.stillGoing(), status + " reports a verdict while claiming to still be running");
        }
    }

    /**
     * The runner declining a case, and a stop putting one back, both report
     * {@code IDLE}. Neither is a verdict and neither is still going, so the claim
     * goes and the case keeps whatever status it had.
     */
    @Test
    public void aCaseThatNeverRanReleasesItsClaimAndRecordsNothing() {
        assertFalse(RunStatus.IDLE.stillGoing());
        assertTrue(RunStatus.IDLE.getVerdict().isEmpty());
    }

    /**
     * A case that has just started records nothing yet, which is why the claim
     * has to survive that first report.
     */
    @Test
    public void aStartIsNotAVerdict() {
        assertTrue(RunStatus.RUNNING.getVerdict().isEmpty());
    }
}
