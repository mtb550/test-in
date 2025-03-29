package com.example.demo;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class TestCaseDetailsToolWindowFactory implements ToolWindowFactory {
    private static TestCaseDetailsPanel instance;

    public static TestCaseDetailsPanel getInstance() {
        return instance;
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        instance = new TestCaseDetailsPanel();
        Content content = ContentFactory.getInstance()
                .createContent(instance.getPanel(), "Details", false);
        toolWindow.getContentManager().addContent(content);

        Content bugsTab = ContentFactory.getInstance()
                .createContent(instance.getBugPanel(), "Open Bugs", false);
        toolWindow.getContentManager().addContent(bugsTab);
    }
}