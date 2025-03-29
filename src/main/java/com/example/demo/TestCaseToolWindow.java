package com.example.demo;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

public class TestCaseToolWindow {
    public static void show(TestCase testCase) {
        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("TestCaseDetails");

        if (toolWindow != null) {
            if (!toolWindow.isVisible()) {
                toolWindow.show();
            }

            TestCaseDetailsPanel viewer = TestCaseDetailsToolWindowFactory.getInstance();
            if (viewer != null) {
                viewer.update(testCase);
            }
        }
    }
}
