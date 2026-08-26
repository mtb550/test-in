package org.testin.run;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;
import org.testin.util.OptionalPlugin;

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

        TestRunner.available().run(p, starting);

        // Once for the click, not once per case: running a page of twelve used
        // to raise twelve balloons. Nothing is said when every case was already
        // running, because nothing was started.
        Services.getInstance(p, Notifier.class).softShowCounted(p, "Running", starting.size());
    }
}
