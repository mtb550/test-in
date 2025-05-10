package com.example.editor;

import com.example.Runner.TestNGRunnerByClassName;
import com.example.demo.TestCaseToolWindow;
import com.example.pojo.TestCase;
import com.example.util.ActionHistory;
import com.example.util.Notifier;
import com.example.util.Tools;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class TableContextMenu {

    public static JBPopupMenu create(JList<TestCase> list,
                                     DefaultListModel<TestCase> model,
                                     TestCase tc) {
        JBPopupMenu menu = new JBPopupMenu();

        JBMenuItem copyItem = new JBMenuItem("📋 Copy");
        copyItem.addActionListener(e -> {
            String text = "Title: " + tc.getTitle() + "\nSteps: " + tc.getSteps() + "\nExpected: " + tc.getExpectedResult();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        });
        menu.add(copyItem);

        JBMenuItem runItem = new JBMenuItem("▶ Run Test");
        runItem.addActionListener(e -> {
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
        });
        menu.add(runItem);

        JBMenuItem viewItem = new JBMenuItem("🔍 View Details");
        viewItem.addActionListener(e -> TestCaseToolWindow.show(tc));
        menu.add(viewItem);

        JBMenuItem deleteItem = new JBMenuItem("🗑 Delete");
        deleteItem.addActionListener(e -> {
            int idx = model.indexOf(tc);
            if (idx >= 0) {
                model.remove(idx);
            }
        });
        menu.add(deleteItem);

        // === 📝 Add Undo button ===
        JBMenuItem undoItem = new JBMenuItem("↩ Undo");
        undoItem.addActionListener(e -> {
            ActionHistory.undo();
        });
        menu.add(undoItem);

        // === 🔁 Add Redo button ===
        JBMenuItem redoItem = new JBMenuItem("↪ Redo");
        redoItem.addActionListener(e -> {
            ActionHistory.redo();
        });
        menu.add(redoItem);

        return menu;
    }

}
