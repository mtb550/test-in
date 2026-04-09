package testGit.util.Broadcasts.listeners;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

public interface TestCaseExecutionListener {

    Topic<TestCaseExecutionListener> TOPIC = Topic.create("RunTestCaseNotification", TestCaseExecutionListener.class);

    void onStatusChanged(@NotNull final String testName, @NotNull final String status, final String error);

}