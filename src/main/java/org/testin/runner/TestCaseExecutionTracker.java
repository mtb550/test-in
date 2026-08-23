package org.testin.runner;

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter;
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.RunStatus;

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
                TestCaseExecutionListener.broadcast(p, test.getPresentableName().toLowerCase(), RunStatus.RUNNING, "");
            }

            @Override
            public void onTestFinished(final @NotNull SMTestProxy test) {
                final @NotNull String testName = test.getPresentableName().toLowerCase();

                if (test.isPassed()) {
                    TestCaseExecutionListener.broadcast(p, testName, RunStatus.PASSED, "");

                } else if (test.isDefect()) {
                    TestCaseExecutionListener.broadcast(p, testName, RunStatus.FAILED,
                            Objects.toString(test.getErrorMessage(), ""));

                } else {
                    TestCaseExecutionListener.broadcast(p, testName, RunStatus.FAILED,
                            Objects.toString(test.getErrorMessage(), "Skipped/Terminated"));
                }
            }
        });
    }
}