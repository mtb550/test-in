package org.testin.runner;

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter;
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.RunStatus;
import org.testin.listeners.TestCaseExecutionListener;

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
                broadcastStatusChange(p, test.getPresentableName().toLowerCase(), RunStatus.RUNNING, null);
            }

            @Override
            public void onTestFinished(final @NotNull SMTestProxy test) {
                String testName = test.getPresentableName().toLowerCase();

                if (test.isPassed())
                    broadcastStatusChange(p, testName, RunStatus.PASSED, null);

                else if (test.isDefect())
                    broadcastStatusChange(p, testName, RunStatus.FAILED, test.getErrorMessage());

                else
                    broadcastStatusChange(p, testName, RunStatus.FAILED, test.getErrorMessage() != null ? test.getErrorMessage() : "Skipped/Terminated");
            }
        });
    }

    private static void broadcastStatusChange(final @NotNull Project p, final @NotNull String testName, final @NotNull RunStatus status, final String error) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!p.isDisposed()) {
                p.getMessageBus().syncPublisher(TestCaseExecutionListener.TOPIC).onStatusChanged(testName, status, error);
            }
        });
    }
}