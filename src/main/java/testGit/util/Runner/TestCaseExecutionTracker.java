package testGit.util.Runner;

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter;
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import testGit.util.listeners.TestCaseExecutionListener;

public class TestCaseExecutionTracker {

    private static boolean isListenerInitialized = false;

    public static void initGlobalListener(@NotNull final Project project) {
        if (isListenerInitialized) return;

        project.getMessageBus().connect().subscribe(SMTRunnerEventsListener.TEST_STATUS, new SMTRunnerEventsAdapter() {
            @Override
            public void onTestStarted(@NotNull final SMTestProxy test) {
                broadcastStatusChange(project, test.getPresentableName().toLowerCase(), "RUNNING", null);
            }

            @Override
            public void onTestFinished(@NotNull final SMTestProxy test) {
                String testName = test.getPresentableName().toLowerCase();

                if (test.isPassed()) broadcastStatusChange(project, testName, "PASSED", null);

                else if (test.isDefect()) broadcastStatusChange(project, testName, "FAILED", test.getErrorMessage());

                else
                    broadcastStatusChange(project, testName, "FAILED", test.getErrorMessage() != null ? test.getErrorMessage() : "Skipped/Terminated");
            }
        });

        isListenerInitialized = true;
    }

    private static void broadcastStatusChange(final @NotNull Project project, @NotNull final String testName, @NotNull final String status, final String error) {
        ApplicationManager.getApplication().invokeLater(() ->
                project.getMessageBus().syncPublisher(TestCaseExecutionListener.TOPIC).onStatusChanged(testName, status, error));
    }
}