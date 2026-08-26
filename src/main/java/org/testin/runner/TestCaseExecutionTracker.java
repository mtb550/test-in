package org.testin.runner;

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter;
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Failure;
import org.testin.model.RunStatus;

import java.time.Duration;

import java.util.Objects;

public class TestCaseExecutionTracker {

    /**
     * Subscribes to the IDE's test events for the life of the project.
     * <p>
     * Called from {@code StartupActivity.execute}, which claims its own run once
     * per project, so this subscribes once without a flag of its own. It carried
     * one while three doors led to startup, and it had to: every extra
     * subscription broadcast each test status change again, once per
     * registration.
     */
    public static void initGlobalListener(final @NotNull Project p) {
        p.getMessageBus().connect(p).subscribe(SMTRunnerEventsListener.TEST_STATUS, new SMTRunnerEventsAdapter() {
            @Override
            public void onTestStarted(final @NotNull SMTestProxy test) {
                TestCaseExecutionListener.broadcast(p, test.getPresentableName().toLowerCase(), RunStatus.RUNNING, Duration.ZERO, Failure.NONE);
            }

            @Override
            public void onTestFinished(final @NotNull SMTestProxy test) {
                final @NotNull String testName = test.getPresentableName().toLowerCase();

                if (test.isPassed()) {
                    TestCaseExecutionListener.broadcast(p, testName, RunStatus.PASSED, durationOf(test), Failure.NONE);

                } else if (test.isDefect()) {
                    TestCaseExecutionListener.broadcast(p, testName, RunStatus.FAILED, durationOf(test), failureOf(test, ""));

                } else {
                    TestCaseExecutionListener.broadcast(p, testName, RunStatus.FAILED, durationOf(test), failureOf(test, "Skipped/Terminated"));
                }
            }
        });
    }

    /**
     * How long the framework says the test took.
     * <p>
     * Taken from the framework rather than timed here because it is the only
     * measurement of the method itself: it excludes the compile, the JVM start
     * and the suite around it, it is exact to the millisecond, and it is per
     * method however many run in one process. The editor's own clock ticks once
     * a second and counts from the click, which is the right measure of a tester
     * working through a case by hand and the wrong one for anything a machine
     * ran.
     * <p>
     * Zero when the platform recorded none - a test whose framework reported no
     * time - which is how a run row is told to keep what it already had.
     */
    private static @NotNull Duration durationOf(final @NotNull SMTestProxy test) {
        final Long millis = test.getDuration();

        return millis == null ? Duration.ZERO : Duration.ofMillis(millis);
    }

    /**
     * What the platform recorded against a finished test, as the value a run row
     * keeps.
     * <p>
     * The fallback is for the case that ended without the framework saying why -
     * skipped, or killed with its process. There is a status but no sentence,
     * and a blank one on the row would read as "this ran and nothing was
     * recorded" rather than as what happened.
     */
    private static @NotNull Failure failureOf(final @NotNull SMTestProxy test, final @NotNull String fallback) {
        return new Failure(Objects.toString(test.getErrorMessage(), fallback), Objects.toString(test.getStacktrace(), ""));
    }
}
