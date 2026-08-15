package org.testin.listeners;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.RunStatus;

public interface TestCaseExecutionListener {

    Topic<TestCaseExecutionListener> TOPIC = Topic.create("RunTestCaseNotification", TestCaseExecutionListener.class);

    void onStatusChanged(final @NotNull String testName, final @NotNull RunStatus status, final String error);

}