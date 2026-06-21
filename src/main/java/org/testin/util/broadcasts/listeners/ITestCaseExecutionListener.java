package org.testin.util.broadcasts.listeners;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

public interface ITestCaseExecutionListener {

    Topic<ITestCaseExecutionListener> TOPIC = Topic.create("RunTestCaseNotification", ITestCaseExecutionListener.class);

    void onStatusChanged(final @NotNull String testName, final @NotNull String status, final String error);

}