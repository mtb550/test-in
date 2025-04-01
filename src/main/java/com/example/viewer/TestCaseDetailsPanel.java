package com.example.viewer;

import com.example.pojo.TestCaseHistory;
import com.example.pojo.DB;
import com.example.pojo.TestCase;
import com.intellij.ui.components.*;

import javax.swing.*;
import java.awt.*;

public class TestCaseDetailsPanel {
    private final JBPanel mainPanel;
    private final JBTabbedPane tabbedPane;

    private final JBPanel detailTab;
    private final JBPanel historyTab;
    private final JBPanel bugTab;

    public TestCaseDetailsPanel() {
        mainPanel = new JBPanel(new BorderLayout());
        tabbedPane = new JBTabbedPane();

        detailTab = new JBPanel(new GridLayout(4, 1));
        historyTab = new JBPanel(new BorderLayout());
        bugTab = new JBPanel(new BorderLayout());

        tabbedPane.addTab("Details", detailTab);
        tabbedPane.addTab("History", historyTab);
        tabbedPane.addTab("Open Bugs", bugTab);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
    }

    public void update(TestCase testCase) {
        detailTab.removeAll();
        historyTab.removeAll();
        bugTab.removeAll();

        detailTab.add(new JBLabel("Title: " + testCase.getTitle()));
        detailTab.add(new JBLabel("Expected: " + testCase.getExpectedResult()));
        detailTab.add(new JBLabel("Steps: " + testCase.getSteps()));
        detailTab.add(new JBLabel("Priority: " + testCase.getPriority()));

        DefaultListModel<String> model = new DefaultListModel<>();
        for (TestCaseHistory history : DB.loadTestCaseHistory()) {
            model.addElement(history.getTimestamp() + " - " + history.getChangeSummary());
        }

        JBList<String> historyList = new JBList<>(model);
        historyTab.add(new JBScrollPane(historyList), BorderLayout.CENTER);

        bugTab.add(new JBLabel("Bug list will be shown here."), BorderLayout.NORTH);

        tabbedPane.setSelectedIndex(0);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public JPanel getDetailsPanel() {
        return detailTab;
    }

    public JPanel getHistoryPanel() {
        return historyTab;
    }

    public JPanel getBugPanel() {
        return bugTab;
    }

}
