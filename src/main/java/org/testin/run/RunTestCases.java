package org.testin.run;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.runner.TestNGExecution;
import org.testin.runner.TestNGRunner;
import org.testin.services.Services;
import org.testin.util.OptionalPlugin;

import java.util.List;

/**
 * Starting test cases: marking them as running, handing each to the runner, and
 * saying so once.
 * <p>
 * Separate from {@link RunTestCaseAction}, which is how a menu offers it. The
 * two callers that already know which cases to run - the card's run icon and the
 * details panel's - used to build an action to reach this, one of them handing
 * it a null list it had no use for (#71).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RunTestCases {

    public static void run(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        if (testCases.isEmpty()) return;
        if (!OptionalPlugin.TESTNG.isAvailableOrWarn(p)) return;

        int started = 0;

        for (final TestCaseDto tc : testCases) {
            if (Services.getInstance(p, TestNGExecution.class).isRunning(tc.getId())) continue;

            // One run per case rather than one run for the selection: a case the
            // tester started from its own card is one they expect to be able to
            // stop on its own, and a run is what a stop reaches.
            Services.getInstance(p, TestNGRunner.class).run(p, List.of(tc));
            started++;
        }

        // Once for the click, not once per case: running a page of twelve used
        // to raise twelve balloons. Nothing is said when every case was already
        // running, because nothing was started.
        if (started > 0) Services.getInstance(p, Notifier.class).softShowCounted(p, "Running", started);
    }
}
