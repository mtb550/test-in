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

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.services.OptionalPlugin;

import java.util.List;

/**
 * Starting test cases: marking them as running, handing them to the runner, and
 * saying so once.
 * <p>
 * Separate from {@link RunTestCaseAction}, which is how a menu offers it. The
 * two callers that already know which cases to run - the card's run icon and the
 * details panel's - used to build an action to reach this, one of them handing
 * it a null list it had no use for (#71).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RunTestCases {

    /**
     * UC-CODEGEN-008, Rule-CODEGEN-031, Rule-CODEGEN-033.
     * <p>
     * Runs a selection as one run.
     * <p>
     * <b>One run for the selection, not one per case.</b> A run is a compile and
     * a JVM, so twelve selected cases used to mean twelve of each, all starting
     * at once - the tester watched "Executing pre-compile tasks" twelve times
     * and the cases ran in parallel, in no order anyone chose. One configuration
     * compiles once and TestNG walks the methods in sequence, which is what
     * running the generated class by hand already did.
     * <p>
     * What it costs is per-case stopping, and the runner already treats that as
     * the ordinary case: one configuration is one process, so stopping a case in
     * a run of twelve stops the eleven beside it - and every one of them is put
     * back, which is exactly what {@code TestNGExecution.stop} does for a test
     * set run today.
     * <p>
     * The order the methods run in is TestNG's, not the tester's: every
     * generated method carries the case's priority, and priority outranks
     * declaration order. Making a run follow the order the tester arranged is a
     * separate decision about what that attribute is for.
     */
    public static void run(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        if (testCases.isEmpty()) return;
        if (!OptionalPlugin.TESTNG.isAvailableOrWarn(p)) return;

        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        // A case already going is left alone rather than started twice. Filtered
        // before the launch, not inside it, so the count below is what actually
        // started.
        final @NotNull List<TestCaseDto> starting = testCases.stream()
                .filter(tc -> !execution.isRunning(tc.getId()))
                .toList();

        if (starting.isEmpty()) return;

        // Nothing is said here. What a run turns out to be is only known a
        // second later, when the runner has looked for each case's generated
        // method, so a count taken at the click said "Running 12" over twelve
        // cases that could not run. The runner says it once it knows - see
        // TestNGExecution.started (#66, finding 18).
        TestRunner.available().run(p, starting);
    }
}
