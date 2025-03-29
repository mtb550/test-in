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
        ContentFactory contentFactory = ContentFactory.getInstance();

        Content detailsTab = contentFactory.createContent(instance.getDetailsPanel(), "Details", false);
        Content historyTab = contentFactory.createContent(instance.getHistoryPanel(), "History", false);
        Content bugsTab = contentFactory.createContent(instance.getBugPanel(), "Open Bugs", false);

        toolWindow.getContentManager().addContent(detailsTab);
        toolWindow.getContentManager().addContent(historyTab);
        toolWindow.getContentManager().addContent(bugsTab);
    }


}