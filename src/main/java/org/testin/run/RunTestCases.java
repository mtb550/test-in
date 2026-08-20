package org.testin.run;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.RunStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.runner.TestCaseExecutionListener;
import org.testin.runner.TestNGRunnerByMethod;
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
            if (tc.getTempStatus() == RunStatus.RUNNING) continue;

            p.getMessageBus().syncPublisher(TestCaseExecutionListener.TOPIC)
                    .onStatusChanged(tc.getId().toString().toLowerCase(), RunStatus.RUNNING, null);

            Services.getInstance(p, TestNGRunnerByMethod.class).runTestMethod(p, tc);
            started++;
        }

        // Once for the click, not once per case: running a page of twelve used
        // to raise twelve balloons. Nothing is said when every case was already
        // running, because nothing was started.
        if (started == 1) Services.getInstance(p, Notifier.class).softShow(p, "Running");
        else if (started > 1) Services.getInstance(p, Notifier.class).softShow(p, "Running " + started);
    }
}
