package org.testin.runner;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.testin.model.RunStatus;

public interface TestCaseExecutionListener {

    Topic<TestCaseExecutionListener> TOPIC = Topic.create("RunTestCaseNotification", TestCaseExecutionListener.class);

    void onStatusChanged(final @NotNull String testName, final @NotNull RunStatus status, final String error);

}