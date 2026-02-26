package testGit.editorPanel.testRunEditor;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import testGit.pojo.GroupType;
import testGit.pojo.TestCase;
import testGit.viewPanel.ViewPanel;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class TestRunCard extends JBPanel<TestRunCard> {
    private static final int CARD_HEIGHT = 160;
    private static final int SELECTED_OPACITY = 220;
    private static final int BORDER_THICKNESS = 1;
    private static TestRunCard currentlySelectedCard = null;
    final TestCase tc;
    private final JBLabel titleLabel = new JBLabel();
    private final JBPanel<?> badgePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0));
    private final JBLabel expectedLabel = createDetailLabel();
    private final JBLabel stepsLabel = createDetailLabel();
    private final JBLabel automationRefLabel = createDetailLabel();
    private final JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, JBUI.scale(8), 8));
    private final Color defaultBackground;
    private final Color selectedBackground;
    private final Border defaultBorder;
    private final Border selectedBorder;
    private boolean isSelected = false;

    public TestRunCard(int index, TestCase tc) {
        super(new BorderLayout());
        this.tc = tc;
        setOpaque(true);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(CARD_HEIGHT)));

        updateBackgrounds(index);
        defaultBackground = getBackground();
        selectedBackground = new JBColor(
                new Color(defaultBackground.getRed(), defaultBackground.getGreen(), defaultBackground.getBlue(), SELECTED_OPACITY),
                new Color(defaultBackground.getRed(), defaultBackground.getGreen(), defaultBackground.getBlue(), SELECTED_OPACITY)
        );

        defaultBorder = JBUI.Borders.empty(BORDER_THICKNESS);

        selectedBorder = BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(new JBColor(new Color(0, 120, 215), new Color(75, 110, 175)), BORDER_THICKNESS),
                JBUI.Borders.empty()
        );

        Font titleFont = JBFont.label().deriveFont(Font.BOLD, UIUtil.getLabelFont().getSize() + 10.0f);
        titleLabel.setFont(titleFont);
        titleLabel.setText(tc.getTitle());
        titleLabel.setForeground(JBColor.namedColor("Label.foreground", UIUtil.getLabelForeground()));

        badgePanel.setOpaque(false);

        BorderLayoutPanel titleLine = new BorderLayoutPanel();
        titleLine.setOpaque(false);
        titleLine.addToLeft(titleLabel);
        titleLine.addToCenter(badgePanel);

        JBPanel<?> content = new JBPanel<>(new VerticalLayout(JBUI.scale(4)));
        content.setOpaque(false);
        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(titleLine);
        content.add(expectedLabel);
        content.add(stepsLabel);
        content.add(automationRefLabel);

        styleActionButtons();
        actionButtonPanel.setVisible(false);

        BorderLayoutPanel wrapper = new BorderLayoutPanel();
        wrapper.setOpaque(false);
        wrapper.setBorder(JBUI.Borders.empty(12, 16));
        wrapper.addToCenter(content);
        wrapper.addToRight(actionButtonPanel);
        add(wrapper, BorderLayout.CENTER);
        setBorder(defaultBorder);
        setupClickListener();
        updateData(index, tc);
    }

    private void styleActionButtons() {
        actionButtonPanel.setOpaque(false);

        actionButtonPanel.add(createModernStatusButton("PASSED",
                new JBColor(new Color(39, 174, 96), new Color(46, 125, 50))));
        actionButtonPanel.add(createModernStatusButton("FAILED",
                new JBColor(new Color(192, 57, 43), new Color(183, 28, 28))));
        actionButtonPanel.add(createModernStatusButton("BLOCKED",
                new JBColor(new Color(243, 156, 18), new Color(237, 108, 2))));
    }

    private JButton createModernStatusButton(String text, Color bg) {
        JButton btn = new JButton(text);

        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.putClientProperty("JButton.backgroundColor", bg);
        btn.putClientProperty("JButton.selectedBackground", bg.darker());

        btn.setBackground(bg);
        btn.setForeground(JBColor.WHITE);
        btn.setFont(JBFont.small().asBold());
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setBorder(JBUI.Borders.empty(6, 16));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bg.brighter());
                btn.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bg);
                btn.repaint();
            }
        });

        return btn;
    }

    private void setupClickListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    ViewPanel.show(TestRunCard.this.tc);
                } else {
                    if (currentlySelectedCard != null && currentlySelectedCard != TestRunCard.this) {
                        currentlySelectedCard.deselect();
                    }

                    isSelected = !isSelected;

                    if (isSelected) {
                        currentlySelectedCard = TestRunCard.this;
                        select();
                    } else {
                        currentlySelectedCard = null;
                        deselect();
                    }
                }
            }
        });
    }

    private void select() {
        actionButtonPanel.setVisible(true);
        setBackground(selectedBackground);
        setBorder(selectedBorder);
        repaint();
    }

    private void deselect() {
        isSelected = false;
        actionButtonPanel.setVisible(false);
        setBackground(defaultBackground);
        setBorder(defaultBorder);
        repaint();
    }

    private void updateBackgrounds(int index) {
        JBColor evenBg = new JBColor(Gray._245, Gray._60);
        JBColor oddBg = new JBColor(Gray._230, Gray._45);
        setBackground(index % 2 == 0 ? evenBg : oddBg);
    }

    public void updateData(int index, TestCase tc) {
        titleLabel.setText(String.format("%d. %s", index + 1, tc.getTitle()));

        expectedLabel.setText("Expected Result: " + tc.getExpectedResult());
        stepsLabel.setText("Steps: " + tc.getSteps());
        automationRefLabel.setText("Automation Reference: " + tc.getAutomationRef());

        updateBackgrounds(index);

        if (isSelected) {
            deselect();
            if (currentlySelectedCard == this) {
                currentlySelectedCard = null;
            }
        } else {
            setBorder(defaultBorder);
        }

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
        return new RoundedBadge(tc.getPriority(), bg, 20);
    }

    private JBLabel createGroupBadge(GroupType groupName) {
        return new RoundedBadge(groupName.name(), JBColor.darkGray, 20);
    }

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
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}