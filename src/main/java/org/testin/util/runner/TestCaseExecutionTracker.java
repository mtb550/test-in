package org.testin.util.runner;

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter;
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.util.broadcasts.listeners.ITestCaseExecutionListener;

public class TestCaseExecutionTracker {

    public static void initGlobalListener(final @NotNull Project project) {
        project.getMessageBus().connect(project).subscribe(SMTRunnerEventsListener.TEST_STATUS, new SMTRunnerEventsAdapter() {
            @Override
            public void onTestStarted(final @NotNull SMTestProxy test) {
                broadcastStatusChange(project, test.getPresentableName().toLowerCase(), "RUNNING", null);
            }

            @Override
            public void onTestFinished(final @NotNull SMTestProxy test) {
                String testName = test.getPresentableName().toLowerCase();

                if (test.isPassed())
                    broadcastStatusChange(project, testName, "PASSED", null);

                else if (test.isDefect())
                    broadcastStatusChange(project, testName, "FAILED", test.getErrorMessage());

                else
                    broadcastStatusChange(project, testName, "FAILED", test.getErrorMessage() != null ? test.getErrorMessage() : "Skipped/Terminated");
            }
        });
    }

    private static void broadcastStatusChange(final @NotNull Project project, final @NotNull String testName, final @NotNull String status, final String error) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) {
                project.getMessageBus().syncPublisher(ITestCaseExecutionListener.TOPIC).onStatusChanged(testName, status, error);
            }
        });
    }
}