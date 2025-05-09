package com.example.viewer;

import com.example.pojo.DB;
import com.example.pojo.TestCase;
import com.example.pojo.TestCaseHistory;
import com.intellij.ui.JBColor;
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

        detailTab.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 16, 8, 16);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;
        addStyledRow("📝 Title:", testCase.getTitle(), detailTab, gbc, row++);
        addStyledRow("🎯 Expected Result:", testCase.getExpectedResult(), detailTab, gbc, row++);
        addStyledRow("🪜 Steps:", testCase.getSteps(), detailTab, gbc, row++);
        addStyledRow("🏷 Priority:", testCase.getPriority(), detailTab, gbc, row++);

        // === History tab ===
        DefaultListModel<String> model = new DefaultListModel<>();
        for (TestCaseHistory history : DB.loadTestCaseHistory()) {
            model.addElement(history.getTimestamp() + " - " + history.getChangeSummary());
        }
        JBList<String> historyList = new JBList<>(model);
        historyList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        historyTab.add(new JBScrollPane(historyList), BorderLayout.CENTER);

        // === Bug tab ===
        bugTab.add(new JBLabel("🐞 Bug list will be shown here."), BorderLayout.NORTH);

        tabbedPane.setSelectedIndex(0);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void addStyledRow(String label, String value, JPanel panel, GridBagConstraints gbc, int row) {
        JBLabel keyLabel = new JBLabel(label);
        keyLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        keyLabel.setHorizontalAlignment(SwingConstants.LEFT);

        JBLabel valueLabel = new JBLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        valueLabel.setForeground(JBColor.foreground());
        valueLabel.setHorizontalAlignment(SwingConstants.LEFT);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(keyLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(valueLabel, gbc);
    }

    private void addLabelAndValue(String label, String value, JPanel panel, GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JBLabel(label), gbc);

        gbc.gridx = 1;
        panel.add(new JBLabel(value), gbc);
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
