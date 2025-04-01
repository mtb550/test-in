package com.example.demo;

import com.example.pojo.DB;
import com.example.pojo.TestCase;
import com.example.pojo.TestCaseHistory;

import javax.swing.*;
import java.awt.*;

public class TestCaseToolWindowContent {
    private final JPanel panel;
    private final JTabbedPane tabbedPane;
    private final JPanel detailTab;
    private final JPanel historyTab;

    public TestCaseToolWindowContent() {
        panel = new JPanel(new BorderLayout());
        tabbedPane = new JTabbedPane();

        detailTab = new JPanel(new GridLayout(4, 1));
        historyTab = new JPanel(new BorderLayout());

        tabbedPane.addTab("Details", detailTab);
        tabbedPane.addTab("History", historyTab);

        panel.add(tabbedPane, BorderLayout.CENTER);
    }

    public void update(TestCase testCase) {
        detailTab.removeAll();
        historyTab.removeAll();

        detailTab.add(new JLabel("Title: " + testCase.getTitle()));
        detailTab.add(new JLabel("Expected: " + testCase.getExpectedResult()));
        detailTab.add(new JLabel("Steps: " + testCase.getSteps()));
        detailTab.add(new JLabel("Priority: " + testCase.getPriority()));

        DefaultListModel<String> model = new DefaultListModel<>();
        for (TestCaseHistory history : DB.loadTestCaseHistory()) {
            model.addElement(history.getTimestamp() + " - " + history.getChangeSummary());
        }
        JList<String> historyList = new JList<>(model);
        historyTab.add(new JScrollPane(historyList), BorderLayout.CENTER);

        tabbedPane.setSelectedIndex(0);
        panel.revalidate();
        panel.repaint();
    }

    public JPanel getPanel() {
        return panel;
    }
}