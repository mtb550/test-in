package com.example.editor;

import com.example.Runner.TestNGRunnerByClassName;
import com.example.Runner.TestNGRunnerBySuite;
import com.example.demo.TestCaseToolWindow;
import com.example.pojo.TestCase;
import com.example.util.Notifier;
import com.example.util.Tools;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;

public class TableContextMenu {

    public TableContextMenu(JPanel card, TestCase tc, List<TestCase> testCases) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem copyItem = new JMenuItem("📋 Copy");
        copyItem.addActionListener(e -> {
            String text = "Title: " + tc.getTitle() + "\nSteps: " + tc.getSteps() + "\nExpected: " + tc.getExpectedResult();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        });

        JMenuItem runItem = new JMenuItem("▶ Run Test");

        runItem.addActionListener(e -> {
            String ref = tc.getAutomationRef();
            Project project = com.intellij.openapi.project.ProjectManager.getInstance().getOpenProjects()[0];
            if (ref != null && !ref.isBlank()) {
                Tools.printTestSourceRoots(project);
                TestNGRunnerByClassName.runTestClass(project, ref);
                TestNGRunnerBySuite.runTestSuite(project, "test.testng.xml");
                Notifier.notify(project, "Test Case Notifications", "Running TestNG class: ", ref, NotificationType.INFORMATION);
            } else {
                Notifier.notify(project, "No automation reference found for this test case.", "", "", NotificationType.WARNING);
            }
        });

        JMenuItem viewItem = new JMenuItem("🔍 View Details");
        viewItem.addActionListener(e -> TestCaseToolWindow.show(tc));

        JMenuItem duplicateItem = new JMenuItem("📄 Duplicate");
        duplicateItem.addActionListener(e -> {
            //testCases.add(new TestCase(tc.getId(), tc.getTitle() + " (Copy)", tc.getExpectedResult(), tc.getSteps(), tc.getPriority(), tc.getAutomationRef(), List.of(GroupType.Regression)));
        });

        JMenuItem deleteItem = new JMenuItem("🗑 Delete");
        deleteItem.addActionListener(e -> {
            testCases.remove(tc);
        });

        menu.add(copyItem);
        menu.add(runItem);
        menu.add(viewItem);
        menu.add(duplicateItem);
        menu.add(deleteItem);

        card.addMouseListener(new CardMouseAdapter(card, menu, tc));
    }

}
