package com.example.viewer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class TestCaseDetailsToolWindowFactory implements ToolWindowFactory {
    @Getter
    private static TestCaseDetailsPanel detailsInstance;

    @Getter
    private static AddTestCasePanel addInstance;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        detailsInstance = new TestCaseDetailsPanel();
        addInstance = new AddTestCasePanel();

        ContentFactory contentFactory = ContentFactory.getInstance();

        // Details tabs
        Content detailsTab = contentFactory.createContent(detailsInstance.getDetailsPanel(), "Details", false);
        Content historyTab = contentFactory.createContent(detailsInstance.getHistoryPanel(), "History", false);
        Content bugsTab = contentFactory.createContent(detailsInstance.getBugPanel(), "Open Bugs", false);

        // Add Test Case tab
        Content addTestCaseTab = contentFactory.createContent(addInstance.getPanel(), "Create Test Case", false);

        toolWindow.getContentManager().addContent(detailsTab);
        toolWindow.getContentManager().addContent(historyTab);
        toolWindow.getContentManager().addContent(bugsTab);
        toolWindow.getContentManager().addContent(addTestCaseTab);

        // === F2 Shortcut Binding ===
        JComponent root = detailsInstance.getPanel();
        KeyStroke f2 = KeyStroke.getKeyStroke("F2");

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_F2) {
                detailsInstance.toggleEditMode(true);
                return true;
            }
            return false;
        });

        root.getActionMap().put("editMode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                detailsInstance.toggleEditMode(true); // enables editing
            }
        });
    }
}