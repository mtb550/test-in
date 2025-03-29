package com.example.demo;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TestCaseDetailsPanel {
    private final JPanel mainPanel;
    private final JPanel bugPanel;
    private final JTabbedPane tabbedPane;
    private final JPanel detailTab;
    private final JPanel historyTab;

    public TestCaseDetailsPanel() {
        mainPanel = new JPanel(new BorderLayout());
        tabbedPane = new JTabbedPane();

        detailTab = new JPanel(new GridLayout(4, 1));
        historyTab = new JPanel(new BorderLayout());
        bugPanel = new JPanel(new BorderLayout());

        tabbedPane.addTab("Details", detailTab);
        tabbedPane.addTab("History", historyTab);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
    }

    public void update(TestCase testCase) {
        detailTab.removeAll();
        historyTab.removeAll();

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

        tabbedPane.setSelectedIndex(0);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public JPanel getBugPanel() {
        bugPanel.removeAll();
        bugPanel.add(new JLabel("Bug list will be shown here."), BorderLayout.NORTH);
        return bugPanel;
    }
}

