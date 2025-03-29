package com.example.demo;

import javax.swing.*;
import java.awt.*;

public class TestCaseDetailsPanel {
    private final JPanel mainPanel;
    private final JTabbedPane tabbedPane;

    private final JPanel detailTab;
    private final JPanel historyTab;
    private final JPanel bugTab;

    public TestCaseDetailsPanel() {
        mainPanel = new JPanel(new BorderLayout());
        tabbedPane = new JTabbedPane();

        detailTab = new JPanel(new GridLayout(4, 1));
        historyTab = new JPanel(new BorderLayout());
        bugTab = new JPanel(new BorderLayout());

        tabbedPane.addTab("Details", detailTab);
        tabbedPane.addTab("History", historyTab);
        tabbedPane.addTab("Open Bugs", bugTab);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
    }

    public void update(TestCase testCase) {
        detailTab.removeAll();
        historyTab.removeAll();
        bugTab.removeAll();

        detailTab.add(new JLabel("Title: " + testCase.getTitle()));
        detailTab.add(new JLabel("Expected: " + testCase.getExpectedResult()));
        detailTab.add(new JLabel("Steps: " + testCase.getSteps()));
        detailTab.add(new JLabel("Priority: " + testCase.getPriority()));

        DefaultListModel<String> model = new DefaultListModel<>();
        for (TestCaseHistory history : DB.loadTestCaseHistory(testCase.getId())) {
            model.addElement(history.getTimestamp() + " - " + history.getChangeSummary());
        }

        JList<String> historyList = new JList<>(model);
        historyTab.add(new JScrollPane(historyList), BorderLayout.CENTER);

        bugTab.add(new JLabel("Bug list will be shown here."), BorderLayout.NORTH);

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
