package org.testin.runner;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.testin.model.RunStatus;

public interface TestCaseExecutionListener {

    Topic<TestCaseExecutionListener> TOPIC = Topic.create("RunTestCaseNotification", TestCaseExecutionListener.class);

    /**
     * @param error what went wrong, and empty when nothing did - every status
     *              other than a failure carries it empty rather than absent, so
     *              no listener has to ask whether there is a message at all
     */
    void onStatusChanged(final @NotNull String testName, final @NotNull RunStatus status, final @NotNull String error);

    /**
     * Tells every screen that a case changed status.
     * <p>
     * One place, because three things say it: the runner starting a case, the
     * platform reporting on one, and a stop putting back the cases that will
     * never report. The disposed check belongs with the publish - a report can
     * land while the project is closing, and it used to be written at only one
     * of the three.
     */
    static void broadcast(final @NotNull Project p, final @NotNull String testName,
                          final @NotNull RunStatus status, final @NotNull String error) {
        if (p.isDisposed()) return;

        p.getMessageBus().syncPublisher(TOPIC).onStatusChanged(testName, status, error);
    }

}