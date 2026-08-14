package org.testin.listeners;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.RunStatus;

public interface ITestCaseExecutionListener {

    Topic<ITestCaseExecutionListener> TOPIC = Topic.create("RunTestCaseNotification", ITestCaseExecutionListener.class);

    void onStatusChanged(final @NotNull String testName, final @NotNull RunStatus status, final String error);

}