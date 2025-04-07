package com.example.editor;

import com.example.Runner.TestNGRunnerByClassName;
import com.example.Runner.TestNGRunnerBySuite;
import com.example.demo.TestCaseToolWindow;
import com.example.pojo.TestCase;
import com.example.util.Notifier;
import com.example.util.Tools;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class TableContextMenu {
    public static JPopupMenu create(JList<TestCase> list,
                                    DefaultListModel<TestCase> model,
                                    TestCase tc) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem copyItem = new JMenuItem("📋 Copy");
        copyItem.addActionListener(e -> {
            String text = "Title: " + tc.getTitle() + "\nSteps: " + tc.getSteps() + "\nExpected: " + tc.getExpectedResult();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        });

        menu.add(copyItem);

        JMenuItem runItem = new JMenuItem("▶ Run Test");
        runItem.addActionListener(e -> {
            String ref = tc.getAutomationRef();
            Project project = ProjectManager.getInstance().getOpenProjects()[0];
            if (ref != null && !ref.isBlank()) {
                Tools.printTestSourceRoots(project);
                TestNGRunnerByClassName.runTestClass(project, ref);
                TestNGRunnerBySuite.runTestSuite(project, "test.testng.xml");
                Notifier.notify(project, "Test Case Notifications", "Running TestNG class: ", ref, NotificationType.INFORMATION);
            } else {
                Notifier.notify(project,
                        "No automation reference found for this test case.",
                        "", "",
                        NotificationType.WARNING);
            }
        });
        menu.add(runItem);

        JMenuItem viewItem = new JMenuItem("🔍 View Details");
        viewItem.addActionListener(e -> TestCaseToolWindow.show(tc));
        menu.add(viewItem);

        JMenuItem deleteItem = new JMenuItem("🗑 Delete");
        deleteItem.addActionListener(e -> {
            int idx = model.indexOf(tc);
            if (idx >= 0) {
                model.remove(idx);
            }
        });
        menu.add(deleteItem);

        return menu;
    }
}
