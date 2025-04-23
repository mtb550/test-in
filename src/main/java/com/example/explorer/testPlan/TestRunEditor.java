package com.example.explorer.testPlan;

import com.example.editor.TestCaseCard;
import com.example.pojo.TestCase;
import com.example.util.sql;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;

import javax.swing.*;

public class TestRunEditor {
    private static JFrame frame;
    private static final JBTabbedPane tabbedPane = new JBTabbedPane();

    public static void open(int testRunId, String title) {
        String tabTitle = "Run: " + title;

        // Check if tab already exists
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabTitle.equals(tabbedPane.getTitleAt(i))) {
                tabbedPane.setSelectedIndex(i);
                getOrCreateWindow().setVisible(true);
                return;
            }
        }

        // Create card panel
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        TestCase[] testCases = new sql().get("""
            SELECT t.* FROM nafath_tc t
            JOIN nafath_tp_testcases map ON map.test_case_id = t.tc_id
            WHERE map.plan_id = ?
            ORDER BY map.run_order
        """, testRunId).as(TestCase[].class);

        for (int i = 0; i < testCases.length; i++) {
            panel.add(new TestCaseCard(i, testCases[i]));
            panel.add(Box.createVerticalStrut(8));
        }

        JBScrollPane scrollPane = new JBScrollPane(panel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // Add new tab
        tabbedPane.addTab(tabTitle, scrollPane);
        tabbedPane.setSelectedComponent(scrollPane);
        getOrCreateWindow().setVisible(true);
    }

    private static JFrame getOrCreateWindow() {
        if (frame != null) return frame;

        frame = new JFrame("Test Run Viewer");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setContentPane(tabbedPane);
        return frame;
    }
}
