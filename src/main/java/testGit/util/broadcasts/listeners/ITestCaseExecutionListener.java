package testGit.util.broadcasts.listeners;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

public interface ITestCaseExecutionListener {

    Topic<ITestCaseExecutionListener> TOPIC = Topic.create("RunTestCaseNotification", ITestCaseExecutionListener.class);

    void onStatusChanged(@NotNull final String testName, @NotNull final String status, final String error);

}