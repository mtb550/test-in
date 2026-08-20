package org.testin.runner;

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter;
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.testin.model.RunStatus;

import java.util.Objects;

public class TestCaseExecutionTracker {

    private static final Key<Boolean> LISTENER_REGISTERED = Key.create("testin.executionListenerRegistered");

    /**
     * Subscribes once per project. {@code StartupActivity.execute} runs both from
     * the startup extension and from the tree tool window factory, so this is
     * called more than once; every extra subscription would broadcast each test
     * status change again, once per registration.
     */
    public static synchronized void initGlobalListener(final @NotNull Project p) {
        if (p.getUserData(LISTENER_REGISTERED) != null) return;
        p.putUserData(LISTENER_REGISTERED, Boolean.TRUE);

        p.getMessageBus().connect(p).subscribe(SMTRunnerEventsListener.TEST_STATUS, new SMTRunnerEventsAdapter() {
            @Override
            public void onTestStarted(final @NotNull SMTestProxy test) {
                TestCaseExecutionListener.broadcast(p, test.getPresentableName().toLowerCase(), RunStatus.RUNNING, "");
            }

            @Override
            public void onTestFinished(final @NotNull SMTestProxy test) {
                final String testName = test.getPresentableName().toLowerCase();

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