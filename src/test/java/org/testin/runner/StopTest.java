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

import org.jetbrains.annotations.NotNull;
import org.testin.model.RunStatus;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class StopTest {

    private static final @NotNull UUID ONE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final @NotNull UUID TWO = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final @NotNull UUID THREE = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    public void aTestCaseStoppedBeforeItsLaunchIsNeverTaken() {
        final RunRegistry registry = new RunRegistry();
        registry.starting(ONE);
        registry.starting(TWO);

        registry.stopping(List.of(ONE));

        assertFalse(registry.take(ONE), "a stopped case was still handed to the launch");
        assertTrue(registry.take(TWO), "the case beside it was dropped from the launch as well");
    }

    @Test
    public void aStopTakesItsOwnRunAndNoOther() {
        final RunRegistry registry = new RunRegistry();
        registry.launched(List.of(ONE, TWO), "cycle 1");
        registry.launched(List.of(THREE), "cycle 2");

        final RunRegistry.Stop stop = registry.stopping(List.of(ONE));

        assertEquals(stop.runs(), Set.of("cycle 1"), "the stop killed the wrong runs");
        assertEquals(Set.copyOf(stop.testCases()), Set.of(ONE, TWO), "a case sharing the process was left running");
        assertTrue(registry.isRunning(THREE), "a case in another run was stopped too");
        assertFalse(registry.isStopped(THREE), "a case in another run was recorded as stopped");
    }

    @Test
    public void aPassedTestCaseKeepsItsVerdictThroughAStop() {
        final RunRegistry registry = new RunRegistry();
        registry.launched(List.of(ONE, TWO), "cycle 1");
        registry.reported(ONE, RunStatus.PASSED);

        final RunRegistry.Stop stop = registry.stopping(List.of(TWO));

        assertEquals(registry.statusOf(ONE), RunStatus.PASSED, "a stop took back a verdict that had already landed");
        assertFalse(stop.testCases().contains(ONE), "a finished case was swept up as a casemate");
        assertFalse(registry.isStopped(ONE), "a finished case was recorded as stopped");
    }

    @Test
    public void stoppingNothingIsItsOwnAnswer() {
        final RunRegistry registry = new RunRegistry();

        assertEquals(registry.stopping(List.of(ONE)), RunRegistry.Stop.NOTHING);
    }

    @Test
    public void aRunThatEndsQuietlyReleasesTheTestCasesItHeld() {
        final RunRegistry registry = new RunRegistry();
        registry.launched(List.of(ONE, TWO), "cycle 1");

        assertEquals(Set.copyOf(registry.ended("cycle 1")), Set.of(ONE, TWO));
        assertFalse(registry.isRunning(ONE), "a case was still running after its process ended");
        assertEquals(registry.statusOf(ONE), RunStatus.IDLE, "a case left behind by a dead process still showed a status");
    }

    @Test
    public void aRunTestinDidNotStartIsLeftAlone() {
        final RunRegistry registry = new RunRegistry();

        assertEquals(registry.ended("someone else's run"), List.of());
    }
}
