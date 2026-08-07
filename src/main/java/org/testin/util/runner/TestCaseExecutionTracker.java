package org.testin.util.runner;

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter;
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.util.broadcasts.listeners.ITestCaseExecutionListener;

public class TestCaseExecutionTracker {

    public static void initGlobalListener(final @NotNull Project p) {
        p.getMessageBus().connect(p).subscribe(SMTRunnerEventsListener.TEST_STATUS, new SMTRunnerEventsAdapter() {
            @Override
            public void onTestStarted(final @NotNull SMTestProxy test) {
                broadcastStatusChange(p, test.getPresentableName().toLowerCase(), "RUNNING", null);
            }

            @Override
            public void onTestFinished(final @NotNull SMTestProxy test) {
                String testName = test.getPresentableName().toLowerCase();

                if (test.isPassed())
                    broadcastStatusChange(p, testName, "PASSED", null);

                else if (test.isDefect())
                    broadcastStatusChange(p, testName, "FAILED", test.getErrorMessage());

                else
                    broadcastStatusChange(p, testName, "FAILED", test.getErrorMessage() != null ? test.getErrorMessage() : "Skipped/Terminated");
            }
        });
    }

    private static void broadcastStatusChange(final @NotNull Project p, final @NotNull String testName, final @NotNull String status, final String error) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!p.isDisposed()) {
                p.getMessageBus().syncPublisher(ITestCaseExecutionListener.TOPIC).onStatusChanged(testName, status, error);
            }
        });
    }
}