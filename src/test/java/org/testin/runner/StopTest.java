package org.testin.runner;

import org.jetbrains.annotations.NotNull;
import org.testin.model.RunStatus;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * UC-CODEGEN-009, Rule-CODEGEN-037.
 * <p>
 * What a stop takes down, and what it leaves alone.
 * <p>
 * #34 asked for its defect to be "reproduced, fixed and covered". It was fixed
 * and never covered, and the criterion was carried onto #66 as finding 20 rather
 * than closed with the issue, so that it stayed visible. This is that coverage.
 * <p>
 * <b>Asked of {@link RunRegistry} rather than of {@code TestNGExecution}.</b>
 * The service needs a {@code Project} - it subscribes to the message bus to hear
 * about a run that ended without saying anything - but the decisions a stop
 * makes are not in the service. They are in the registry, which is a set of maps
 * over ids and holds nothing from the IDE at all. So the state machine is tested
 * for what it decides, and what genuinely needs a running IDE - killing a
 * process - stays where only a sandbox can check it.
 */
public class StopTest {

    private static final @NotNull UUID ONE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final @NotNull UUID TWO = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final @NotNull UUID THREE = UUID.fromString("00000000-0000-0000-0000-000000000003");

    /**
     * A case stopped between being asked for and being launched never launches.
     * <p>
     * {@code stillWanted} filters a selection through {@code take}, immediately
     * before the configuration is built - so this is the difference between a
     * case that is left out of the run and one that is started and then killed.
     */
    @Test
    public void aCaseStoppedBeforeItsLaunchIsNeverTaken() {
        final RunRegistry registry = new RunRegistry();
        registry.starting(ONE);
        registry.starting(TWO);

        registry.stopping(List.of(ONE));

        assertFalse(registry.take(ONE), "a stopped case was still handed to the launch");
        assertTrue(registry.take(TWO), "the case beside it was dropped from the launch as well");
    }

    /**
     * A stop reaches the run it was aimed at and leaves the others alone.
     * <p>
     * One configuration is one process, so a case in a run of twelve cannot be
     * stopped without the eleven beside it - and those coming back to Running
     * would be the original defect in a smaller place. A different run is not
     * touched.
     */
    @Test
    public void aStopTakesItsOwnRunAndNoOther() {
        final RunRegistry registry = new RunRegistry();
        registry.launched(List.of(ONE, TWO), "cycle 1");
        registry.launched(List.of(THREE), "cycle 2");

        final RunRegistry.Stop stop = registry.stopping(List.of(ONE));

        assertEquals(stop.runs(), Set.of("cycle 1"), "the stop killed the wrong runs");
        assertEquals(Set.copyOf(stop.cases()), Set.of(ONE, TWO), "a case sharing the process was left running");
        assertTrue(registry.isRunning(THREE), "a case in another run was stopped too");
        assertFalse(registry.isStopped(THREE), "a case in another run was recorded as stopped");
    }

    /**
     * A case that already reported keeps its verdict when a stop sweeps the
     * selection it is in.
     * <p>
     * A verdict is the whole point of the run. It survives because a reported
     * case is no longer in {@code configOf}, so the sweep that collects the
     * casemates does not reach it - which is worth pinning, because it is a
     * property of how the sweep is written rather than a check it performs.
     */
    @Test
    public void aPassedCaseKeepsItsVerdictThroughAStop() {
        final RunRegistry registry = new RunRegistry();
        registry.launched(List.of(ONE, TWO), "cycle 1");
        registry.reported(ONE, RunStatus.PASSED);

        final RunRegistry.Stop stop = registry.stopping(List.of(TWO));

        assertEquals(registry.statusOf(ONE), RunStatus.PASSED, "a stop took back a verdict that had already landed");
        assertFalse(stop.cases().contains(ONE), "a finished case was swept up as a casemate");
        assertFalse(registry.isStopped(ONE), "a finished case was recorded as stopped");
    }

    /**
     * Stopping a selection that is not running does nothing at all.
     * <p>
     * Its own answer rather than an empty one a caller has to test for: the
     * runs without the cases kills processes and leaves cards showing Running,
     * and the cases without the runs puts the cards back while the tests keep
     * going.
     */
    @Test
    public void stoppingNothingIsItsOwnAnswer() {
        final RunRegistry registry = new RunRegistry();

        assertEquals(registry.stopping(List.of(ONE)), RunRegistry.Stop.NOTHING);
    }

    /**
     * A run whose process ends without reporting gives its cases back.
     * <p>
     * A failed build, a crashed JVM and the IDE's own Stop button all end a
     * process without a verdict this registry would otherwise hear. A case left
     * behind reads as running for the rest of the session.
     */
    @Test
    public void aRunThatEndsQuietlyReleasesTheCasesItHeld() {
        final RunRegistry registry = new RunRegistry();
        registry.launched(List.of(ONE, TWO), "cycle 1");

        assertEquals(Set.copyOf(registry.ended("cycle 1")), Set.of(ONE, TWO));
        assertFalse(registry.isRunning(ONE), "a case was still running after its process ended");
        assertEquals(registry.statusOf(ONE), RunStatus.IDLE, "a case left behind by a dead process still showed a status");
    }

    /**
     * And a run this plugin did not start is not ours to give back.
     * <p>
     * A tester's own configuration can carry the same name as ours; only runs
     * launched here are tracked, so only those are released.
     */
    @Test
    public void aRunTestinDidNotStartIsLeftAlone() {
        final RunRegistry registry = new RunRegistry();

        assertEquals(registry.ended("someone else's run"), List.of());
    }
}
