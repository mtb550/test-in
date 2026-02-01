package testGit.editorPanel;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.TestCase;

import javax.swing.*;
import java.awt.*;

public class TestCaseCard extends JPanel {
    public TestCaseCard(int index, TestCase tc) {
        // infinite sout
        //System.out.println("TestCaseCard.TestCaseCard()");
        setLayout(new BorderLayout(10, 10));

        setBackground(index % 2 == 0
                ? new JBColor(Gray._245, Gray._60)  // even row
                : new JBColor(Gray._230, Gray._45)  // odd row

        );
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Gray._60),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

        JBLabel title = new JBLabel("#" + (index + 1) + ". " + tc.getTitle());
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(new JBColor(
                Color.DARK_GRAY,   // for light theme
                Color.LIGHT_GRAY   // for dark theme
        ));

        // Align the component to the left
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JBLabel expected = new JBLabel("Expected: " + tc.getExpectedResult());
        expected.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        expected.setForeground(new JBColor(
                Color.DARK_GRAY,   // for light theme
                Color.LIGHT_GRAY   // for dark theme
        ));
        expected.setAlignmentX(Component.LEFT_ALIGNMENT);

        JBLabel steps = new JBLabel("Steps: " + tc.getSteps());
        steps.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        steps.setForeground(new JBColor(
                Color.DARK_GRAY,   // for light theme
                Color.LIGHT_GRAY   // for dark theme
        ));
        steps.setAlignmentX(Component.LEFT_ALIGNMENT);

        JBLabel automationRef = new JBLabel("Automation Ref: " + tc.getAutomationRef());
        automationRef.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        automationRef.setForeground(new JBColor(
                Color.DARK_GRAY,   // for light theme
                Color.LIGHT_GRAY   // for dark theme
        ));
        automationRef.setAlignmentX(Component.LEFT_ALIGNMENT);

        JBLabel priorityBadge = getJbLabel(tc);

        // Panel for the title and priority side-by-side
        JBPanel<?> titleLine = new JBPanel<>();
        titleLine.setLayout(new BoxLayout(titleLine, BoxLayout.X_AXIS));
        titleLine.setOpaque(false);
        // Ensure the line itself is left-aligned
        titleLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleLine.add(title);
        titleLine.add(Box.createHorizontalStrut(8));
        titleLine.add(priorityBadge);

        // Main content panel stacked vertically
        JBPanel<?> content = new JBPanel<>();
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

    private static @NotNull JBLabel getJbLabel(TestCase tc) {
        // infinite sout
        //System.out.println("TestCaseCard.getJbLabel()");

        JBLabel priorityBadge = new JBLabel(tc.getPriority().toUpperCase());
        priorityBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        priorityBadge.setOpaque(true);
        priorityBadge.setForeground(new JBColor(
                Color.DARK_GRAY,   // for light theme
                Color.LIGHT_GRAY   // for dark theme
        ));
        priorityBadge.setBackground(switch (tc.getPriority().toLowerCase()) {
            case "high" -> JBColor.ORANGE;
            case "medium" -> JBColor.magenta;
            default -> JBColor.green;
        });
        priorityBadge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        priorityBadge.setHorizontalAlignment(SwingConstants.CENTER);
        priorityBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
        return priorityBadge;
    }
}
