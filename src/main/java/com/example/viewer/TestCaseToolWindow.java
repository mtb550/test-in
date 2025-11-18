package com.example.viewer;

import com.example.pojo.TestCase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;

import java.util.function.Consumer;

public class TestCaseToolWindow {

    public static void addTestCase(Consumer<TestCase> onSaveCallback) {
        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("TestCaseDetails"); // in plugin.xml <toolWindow id="TestCaseDetails"

        if (toolWindow != null) {
            if (!toolWindow.isVisible()) {
                toolWindow.show();
            }

            // Switch to "Create Test Case" tab
            Content[] contents = toolWindow.getContentManager().getContents();
            for (Content content : contents) {
                if ("Create Test Case".equals(content.getDisplayName())) {
                    toolWindow.getContentManager().setSelectedContent(content);
                    break;
                }
            }

            AddTestCasePanel add = TestCaseDetailsToolWindowFactory.getAddInstance();
            if (add != null) {
                add.setOnSaveCallback(onSaveCallback);
            }
        }
    }

    public static void show(TestCase testCase) {
        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("TestCaseDetails"); // in plugin.xml <toolWindow id="TestCaseDetails"

        if (toolWindow != null) {
            if (!toolWindow.isVisible()) {
                toolWindow.show();
            }

            // Switch to "Details" tab
            Content[] contents = toolWindow.getContentManager().getContents();
            for (Content content : contents) {
                if ("Details".equals(content.getDisplayName())) {
                    toolWindow.getContentManager().setSelectedContent(content);
                    break;
                }
            }

            TestCaseDetailsPanel viewer = TestCaseDetailsToolWindowFactory.getDetailsInstance();
            if (viewer != null) {
                viewer.update(testCase);
            }
        }
    }
}