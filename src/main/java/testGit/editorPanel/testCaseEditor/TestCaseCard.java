package testGit.editorPanel.testCaseEditor;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import testGit.pojo.GroupType;
import testGit.pojo.TestCase;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TestCaseCard extends JBPanel<TestCaseCard> {
    private final JBLabel titleLabel = new JBLabel();
    private final JBPanel<?> badgePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(10), 0));
    private final JBLabel expectedLabel = createDetailLabel();
    private final JBLabel stepsLabel = createDetailLabel();
    private final JBLabel automationRefLabel = createDetailLabel();

    public TestCaseCard() {
        // إعداد الهيكل الأساسي
        setLayout(new BorderLayout()); // استخدام BorderLayout للطبقة الخارجية
        setOpaque(true);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(160)));

        Font titleFont = JBFont.label().deriveFont(Font.BOLD, UIUtil.getLabelFont().getSize() + 10.0f);
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(UIUtil.getLabelForeground());
        badgePanel.setOpaque(false);

        JBPanel<?> titleLine = new JBPanel<>(new BorderLayout());
        titleLine.setOpaque(false);

        titleLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleLine.add(titleLabel, BorderLayout.WEST);
        titleLine.add(badgePanel, BorderLayout.CENTER);

        JBPanel<?> content = new JBPanel<>();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(titleLine);
        content.add(Box.createVerticalStrut(JBUI.scale(8)));
        content.add(expectedLabel);
        content.add(stepsLabel);
        content.add(Box.createVerticalStrut(JBUI.scale(4)));
        content.add(automationRefLabel);

        JBPanel<?> wrapper = new JBPanel<>(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(JBUI.Borders.empty(12));
        wrapper.add(content, BorderLayout.CENTER);

        add(wrapper, BorderLayout.CENTER);
    }

    public void updateData(int index, TestCase tc) {
        titleLabel.setText((index + 1) + ". " + tc.getTitle());
        expectedLabel.setText("Expected Result: " + tc.getExpectedResult());
        stepsLabel.setText("Steps: " + tc.getSteps());
        automationRefLabel.setText("Automation Reference: " + tc.getAutomationRef());

        setBackground(index % 2 == 0 ? new JBColor(Gray._245, Gray._60) : new JBColor(Gray._230, Gray._45));
        setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 1, 0));

        badgePanel.removeAll();
        badgePanel.add(createPriorityBadge(tc));
        List<GroupType> groups = tc.getGroups();
        if (groups != null) {
            for (GroupType groupName : groups) {
                badgePanel.add(createGroupBadge(groupName));
            }
        }
    }

    private JBLabel createPriorityBadge(TestCase tc) {
        Color bg = switch (tc.getPriority().toLowerCase()) {
            case "high" -> JBColor.CYAN;
            case "medium" -> JBColor.magenta;
            default -> JBColor.lightGray;
        };
        //JBLabel badge = new JBLabel(tc.getPriority().toUpperCase());
        //setupBadgeStyle(badge, bg);
        //return badge;
        return new RoundedBadge(tc.getPriority(), bg, 20);
    }

    private JBLabel createGroupBadge(GroupType groupName) {
        //JBLabel badge = new JBLabel(groupName.name());
        //setupBadgeStyle(badge, JBColor.darkGray);
        return new RoundedBadge(groupName.name(), JBColor.darkGray, 20);
        //return badge;
    }

//    private void setupBadgeStyle(JBLabel badge, Color bg) {
//        badge.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL).deriveFont(Font.BOLD));
//        badge.setOpaque(true);
//        badge.setForeground(JBColor.WHITE);
//        badge.setBackground(bg);
//        badge.setBorder(JBUI.Borders.empty(2, 8));
//    }


    private JBLabel createDetailLabel() {
        JBLabel label = new JBLabel();
        label.setFont(UIUtil.getLabelFont(UIUtil.FontSize.NORMAL));
        label.setForeground(UIUtil.getContextHelpForeground());

        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    private static class RoundedBadge extends JBLabel {
        private final int radius;

        RoundedBadge(String text, Color bg, int radius) {
            super(text.toUpperCase());
            this.radius = radius;
            setOpaque(false);
            setBackground(bg);
            setForeground(JBColor.WHITE);
            setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL).deriveFont(Font.BOLD));
            setBorder(JBUI.Borders.empty(2, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            // رسم الخلفية المنحنية
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}