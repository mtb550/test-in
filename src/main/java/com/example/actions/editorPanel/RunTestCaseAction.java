package com.example.actions.editorPanel;

import com.example.Runner.TestNGRunnerByClassName;
import com.example.pojo.TestCase;
import com.example.util.Notifier;
import com.example.util.Tools;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;

public class RunTestCaseAction extends AnAction {
    TestCase tc;

    public RunTestCaseAction(TestCase tc) {
        super("▶ Run Test");
        this.tc = tc;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String ref = tc.getAutomationRef();
        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        if (ref != null && !ref.isBlank()) {
            Tools.printTestSourceRoots(project);
            TestNGRunnerByClassName.runTestClass(project, ref);
            Notifier.notify(project, "Test Case Notifications", "Running TestNG class: ", ref, NotificationType.INFORMATION);
        } else {
            Notifier.notify(project,
                    "No automation reference found for this test case.",
                    "", "",
                    NotificationType.WARNING);
        }
    }
}
