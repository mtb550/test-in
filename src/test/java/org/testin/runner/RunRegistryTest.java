/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.runner;

import org.testin.model.RunStatus;
import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class RunRegistryTest {

    private static final String RUN = "Testin: three cases";

    private static TestCaseDto reloaded(final TestCaseDto original) {
        return TestCaseDto.builder().id(original.getId()).description(original.getDescription()).build();
    }

    private static TestCaseDto aTestCase(final String description) {
        return TestCaseDto.builder().description(description).build();
    }

    @Test
    public void aStopReachesATestCaseThroughAnyNumberOfReloads() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto original = aTestCase("logs in");

        registry.starting(original.getId());
        registry.take(original.getId());
        registry.launched(List.of(original.getId()), RUN);

        final TestCaseDto afterReload = reloaded(reloaded(reloaded(original)));
        assertNotSame(afterReload, original, "a rescan hands the editors a new instance");

        assertTrue(registry.isRunning(afterReload.getId()), "the fresh instance is the same running case");

        final RunRegistry.Stop stop = registry.stopping(List.of(afterReload.getId()));
        assertEquals(stop.testCases(), List.of(original.getId()), "and the stop reaches it");
        assertEquals(stop.runs(), Set.of(RUN), "naming the run whose process has to be killed");
    }

    @Test
    public void aTestCaseThatWasNeverRunningIsNotStopped() {
        final RunRegistry registry = new RunRegistry();

        final RunRegistry.Stop stop = registry.stopping(List.of(UUID.randomUUID()));

        assertSame(stop, RunRegistry.Stop.NOTHING, "nothing to kill and nothing to repaint");
        assertTrue(stop.testCases().isEmpty(), "so no case is put back");
    }

    @Test
    public void stoppingOneTestCaseStopsTheOnesSharingItsProcess() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto first = aTestCase("first");
        final TestCaseDto second = aTestCase("second");
        final TestCaseDto alone = aTestCase("in a run of its own");

        registry.launched(List.of(first.getId(), second.getId()), RUN);
        registry.launched(List.of(alone.getId()), "Testin: one case");

        final RunRegistry.Stop stop = registry.stopping(List.of(first.getId()));

        assertTrue(stop.testCases().contains(second.getId()), "one configuration is one process, so its casemate goes too");
        assertFalse(stop.testCases().contains(alone.getId()), "but a case in another run is left alone");
        assertTrue(registry.isStopped(second.getId()), "and a report arriving for it afterward is not a failure");
    }

    @Test
    public void aTestCaseThatAlreadyReportedIsNotSweptUpByALaterStop() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto finished = aTestCase("finished early");
        final TestCaseDto stillGoing = aTestCase("still going");

        registry.launched(List.of(finished.getId(), stillGoing.getId()), RUN);
        registry.reported(finished.getId(), RunStatus.PASSED);

        final RunRegistry.Stop stop = registry.stopping(List.of(stillGoing.getId()));

        assertFalse(stop.testCases().contains(finished.getId()), "its verdict is in, so the stop is not about it");
        assertEquals(registry.statusOf(finished.getId()), RunStatus.PASSED, "and the verdict it gave still stands");
    }

    @Test
    public void aTestCaseStoppedBeforeItsLaunchIsLeftOutOfTheRun() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto tc = aTestCase("stopped in the second before the process");

        registry.starting(tc.getId());
        registry.stopping(List.of(tc.getId()));

        assertFalse(registry.take(tc.getId()), "the launch finds it gone when its turn comes");
    }

    @Test
    public void runningAgainClearsTheStopThatEndedTheLastRun() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto tc = aTestCase("run, stopped, run again");

        registry.launched(List.of(tc.getId()), RUN);
        registry.stopping(List.of(tc.getId()));
        assertTrue(registry.isStopped(tc.getId()), "its next report belongs to the run that was killed");

        registry.starting(tc.getId());
        assertFalse(registry.isStopped(tc.getId()), "but this is a new run, and its reports are real");
    }

    @Test
    public void aVerdictSurvivesAReload() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto original = aTestCase("passes");

        registry.launched(List.of(original.getId()), RUN);
        registry.reported(original.getId(), RunStatus.PASSED);

        assertEquals(registry.statusOf(reloaded(original).getId()), RunStatus.PASSED,
                "the green badge does not vanish at the tester's next keystroke");
    }

    @Test
    public void aTestCaseThatNeverStartedKeepsItsLastVerdict() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto tc = aTestCase("passed earlier, run again during indexing");

        registry.reported(tc.getId(), RunStatus.PASSED);
        registry.notStarting(tc.getId());
        registry.reported(tc.getId(), RunStatus.IDLE);

        assertEquals(registry.statusOf(tc.getId()), RunStatus.PASSED, "a run that never started wiped the last verdict");
    }

    @Test
    public void aStoppedTestCaseIsRecordedAsNotRun() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto tc = aTestCase("passed earlier, stopped this time");

        registry.reported(tc.getId(), RunStatus.PASSED);
        registry.starting(tc.getId());
        registry.take(tc.getId());
        registry.launched(List.of(tc.getId()), RUN);
        registry.stopping(List.of(tc.getId()));
        registry.reported(tc.getId(), RunStatus.IDLE);

        assertEquals(registry.statusOf(tc.getId()), RunStatus.IDLE, "a stopped case kept the verdict of the run before");
    }

    @Test
    public void aTestCaseNobodyHasRunIsIdle() {
        final RunRegistry registry = new RunRegistry();

        final RunStatus status = registry.statusOf(UUID.randomUUID());

        assertEquals(status, RunStatus.IDLE, "the empty answer is a status of its own, so no caller checks for one");
        assertFalse(status.hasBadge(), "and it draws nothing");
    }

    @Test
    public void runningBeatsWhateverTheLastRunSaid() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto tc = aTestCase("failed once, running again");

        registry.reported(tc.getId(), RunStatus.FAILED);
        registry.starting(tc.getId());

        assertEquals(registry.statusOf(tc.getId()), RunStatus.RUNNING, "the card shows what it is doing now");
    }

    @Test
    public void aReportOfRunningIsNotAVerdict() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto tc = aTestCase("reports itself started");

        registry.launched(List.of(tc.getId()), RUN);
        registry.reported(tc.getId(), RunStatus.RUNNING);

        assertTrue(registry.isRunning(tc.getId()), "a case does not stop running by saying that it is");
    }

    @Test
    public void aRunThatEndsWithoutReportingPutsItsTestCasesBack() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto first = aTestCase("never reached");
        final TestCaseDto second = aTestCase("never reached either");

        registry.launched(List.of(first.getId(), second.getId()), RUN);

        final List<UUID> abandoned = registry.ended(RUN);

        assertEquals(abandoned.size(), 2, "a build that failed reports nothing, and both cases were waiting on it");
        assertFalse(registry.isRunning(first.getId()), "so neither is left showing Running for the session");
        assertFalse(registry.isRunning(second.getId()), "which is what the maps used to do");
    }

    @Test
    public void aRunThatEndedWithEveryResultInLeavesNothingBehind() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto tc = aTestCase("passes");

        registry.launched(List.of(tc.getId()), RUN);
        registry.reported(tc.getId(), RunStatus.PASSED);

        assertTrue(registry.ended(RUN).isEmpty(), "nothing to put back, so no card is repainted");
        assertEquals(registry.statusOf(tc.getId()), RunStatus.PASSED, "and the verdict is untouched by the run ending");
    }

    @Test
    public void aRunThisPluginDidNotStartIsIgnored() {
        final RunRegistry registry = new RunRegistry();

        assertTrue(registry.ended("A build the tester left going").isEmpty(),
                "a configuration of the tester's own is theirs, whatever it is called");
    }

    @Test
    public void aRunStopsBeingHeldOnceItEnds() {
        final RunRegistry registry = new RunRegistry();
        final TestCaseDto tc = aTestCase("one run");

        registry.launched(List.of(tc.getId()), RUN);
        assertTrue(registry.launchedHere(RUN), "the stop may kill this one");

        registry.ended(RUN);
        assertFalse(registry.launchedHere(RUN), "and the name is not kept for the rest of the session");
    }
}
