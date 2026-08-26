package org.testin.runner;

import org.testin.model.RunStatus;
import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * The runner's bookkeeping survives an editor reload (#116).
 * <p>
 * Two defects had the same shape: state kept against a {@code TestCaseDto}
 * instance, when an indexer rescan replaces every instance the editors hold. A
 * stop then found nothing to kill and the run carried on; a badge then found
 * nothing to paint and a case that had just passed went blank. The registry
 * keys by the case's id, which outlives the reload, and these tests build a
 * second DTO with the same id to say so.
 * <p>
 * Testable at all because the registry knows nothing of the platform: there are
 * no IDE fixtures in this test tree, and the half of the runner that launches
 * and kills processes cannot be built without a project.
 */
public class RunRegistryTest {

    private static final String RUN = "Testin: three cases";

    /**
     * The same case as the editors would hold it after a rescan: a different
     * object, carrying the same id.
     */
    private static TestCaseDto reloaded(final TestCaseDto original) {
        return TestCaseDto.builder().id(original.getId()).description(original.getDescription()).build();
    }

    private static TestCaseDto aCase(final String description) {
        return TestCaseDto.builder().description(description).build();
    }

    // ------------------------------------------------------------ #116, the stop

    @Test
    public void aStopReachesACaseThroughAnyNumberOfReloads() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto original = aCase("logs in");

        registry.starting(original.getId());
        registry.take(original.getId());
        registry.launched(List.of(original.getId()), RUN);

        final TestCaseDto afterReload = reloaded(reloaded(reloaded(original)));
        assertNotSame(afterReload, original, "a rescan hands the editors a new instance");

        assertTrue(registry.isRunning(afterReload.getId()), "the fresh instance is the same running case");

        final RunRegistry.Stop stop = registry.stopping(List.of(afterReload.getId()));
        assertEquals(stop.cases(), List.of(original.getId()), "and the stop reaches it");
        assertEquals(stop.runs(), Set.of(RUN), "naming the run whose process has to be killed");
    }

    @Test
    public void aCaseThatWasNeverRunningIsNotStopped() {
        final RunRegistry registry = new RunRegistry();

        final RunRegistry.Stop stop = registry.stopping(List.of(UUID.randomUUID()));

        assertSame(stop, RunRegistry.Stop.NOTHING, "nothing to kill and nothing to repaint");
        assertTrue(stop.cases().isEmpty(), "so no case is put back");
    }

    @Test
    public void stoppingOneCaseStopsTheOnesSharingItsProcess() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto first = aCase("first");
        final TestCaseDto second = aCase("second");
        final TestCaseDto alone = aCase("in a run of its own");

        registry.launched(List.of(first.getId(), second.getId()), RUN);
        registry.launched(List.of(alone.getId()), "Testin: one case");

        final RunRegistry.Stop stop = registry.stopping(List.of(first.getId()));

        assertTrue(stop.cases().contains(second.getId()), "one configuration is one process, so its casemate goes too");
        assertFalse(stop.cases().contains(alone.getId()), "but a case in another run is left alone");
        assertTrue(registry.isStopped(second.getId()), "and a report arriving for it afterward is not a failure");
    }

    @Test
    public void aCaseThatAlreadyReportedIsNotSweptUpByALaterStop() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto finished = aCase("finished early");
        final TestCaseDto stillGoing = aCase("still going");

        registry.launched(List.of(finished.getId(), stillGoing.getId()), RUN);
        registry.reported(finished.getId(), RunStatus.PASSED);

        final RunRegistry.Stop stop = registry.stopping(List.of(stillGoing.getId()));

        assertFalse(stop.cases().contains(finished.getId()), "its verdict is in, so the stop is not about it");
        assertEquals(registry.statusOf(finished.getId()), RunStatus.PASSED, "and the verdict it gave still stands");
    }

    @Test
    public void aCaseStoppedBeforeItsLaunchIsLeftOutOfTheRun() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto tc = aCase("stopped in the second before the process");

        registry.starting(tc.getId());
        registry.stopping(List.of(tc.getId()));

        assertFalse(registry.take(tc.getId()), "the launch finds it gone when its turn comes");
    }

    @Test
    public void runningAgainClearsTheStopThatEndedTheLastRun() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto tc = aCase("run, stopped, run again");

        registry.launched(List.of(tc.getId()), RUN);
        registry.stopping(List.of(tc.getId()));
        assertTrue(registry.isStopped(tc.getId()), "its next report belongs to the run that was killed");

        registry.starting(tc.getId());
        assertFalse(registry.isStopped(tc.getId()), "but this is a new run, and its reports are real");
    }

    // ------------------------------------------- the verdict outlives the reload

    @Test
    public void aVerdictSurvivesAReload() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto original = aCase("passes");

        registry.launched(List.of(original.getId()), RUN);
        registry.reported(original.getId(), RunStatus.PASSED);

        assertEquals(registry.statusOf(reloaded(original).getId()), RunStatus.PASSED,
                "the green badge does not vanish at the tester's next keystroke");
    }

    @Test
    public void aCaseNobodyHasRunIsIdle() {
        final RunRegistry registry = new RunRegistry();

        final RunStatus status = registry.statusOf(UUID.randomUUID());

        assertEquals(status, RunStatus.IDLE, "the empty answer is a status of its own, so no caller checks for one");
        assertFalse(status.hasBadge(), "and it draws nothing");
    }

    @Test
    public void runningBeatsWhateverTheLastRunSaid() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto tc = aCase("failed once, running again");

        registry.reported(tc.getId(), RunStatus.FAILED);
        registry.starting(tc.getId());

        assertEquals(registry.statusOf(tc.getId()), RunStatus.RUNNING, "the card shows what it is doing now");
    }

    @Test
    public void aReportOfRunningIsNotAVerdict() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto tc = aCase("reports itself started");

        registry.launched(List.of(tc.getId()), RUN);
        registry.reported(tc.getId(), RunStatus.RUNNING);

        assertTrue(registry.isRunning(tc.getId()), "a case does not stop running by saying that it is");
    }
}
