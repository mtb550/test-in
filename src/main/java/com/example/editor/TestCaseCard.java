package com.example.editor;

import com.example.pojo.TestCase;

import javax.swing.*;
import java.awt.*;

public class TestCaseCard extends JPanel {
    public TestCaseCard(int index, TestCase tc) {
        setLayout(new BorderLayout(10, 10));
        setBackground(index % 2 == 0 ? new Color(40, 40, 40) : new Color(50, 50, 50));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

        JLabel title = new JLabel("#" + (index + 1) + ". " + tc.getTitle());
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(Color.WHITE);
        // Align the component to the left
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel expected = new JLabel("Expected: " + tc.getExpectedResult());
        expected.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        expected.setForeground(Color.LIGHT_GRAY);
        expected.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel steps = new JLabel("Steps: " + tc.getSteps());
        steps.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        steps.setForeground(Color.LIGHT_GRAY);
        steps.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel automationRef = new JLabel("Automation Ref: " + tc.getAutomationRef());
        automationRef.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        automationRef.setForeground(new Color(200, 200, 255));
        automationRef.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel priorityBadge = new JLabel(tc.getPriority().toUpperCase());
        priorityBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        priorityBadge.setOpaque(true);
        priorityBadge.setForeground(Color.WHITE);
        priorityBadge.setBackground(switch (tc.getPriority().toLowerCase()) {
            case "high" -> new Color(219, 68, 55);
            case "medium" -> new Color(255, 193, 7);
            case "low" -> new Color(76, 175, 80);
            default -> new Color(120, 120, 120);
        });
        priorityBadge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        priorityBadge.setHorizontalAlignment(SwingConstants.CENTER);
        priorityBadge.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Panel for the title and priority side-by-side
        JPanel titleLine = new JPanel();
        titleLine.setLayout(new BoxLayout(titleLine, BoxLayout.X_AXIS));
        titleLine.setOpaque(false);
        // Ensure the line itself is left-aligned
        titleLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleLine.add(title);
        titleLine.add(Box.createHorizontalStrut(8));
        titleLine.add(priorityBadge);

        // Main content panel stacked vertically
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        // Ensure content panel is left-aligned
        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add components in vertical order
        content.add(titleLine);
        content.add(Box.createVerticalStrut(6));
        content.add(expected);
        content.add(steps);
        content.add(automationRef);

        // Add the content panel to the main panel
        add(content, BorderLayout.CENTER);
    }
}
