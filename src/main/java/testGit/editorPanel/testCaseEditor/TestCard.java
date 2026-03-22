package testGit.editorPanel.testCaseEditor;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import testGit.editorPanel.Shared;
import testGit.pojo.GroupType;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

public class TestCard extends JBPanel<TestCard> {
    private static final int CARD_HEIGHT = 130;
    private final JBLabel titleLabel = new JBLabel();
    private final JBPanel<?> badgePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(10), 0));
    private final JBLabel expectedLabel = createDetailLabel();
    private final JBLabel stepsLabel = createDetailLabel();
    private final JBLabel automationRefLabel = createDetailLabel();
    private final JBLabel businessRefLabel = createDetailLabel();
    private final JBLabel moduleLabel = createDetailLabel();
    private final JBLabel idLabel = createDetailLabel();
    private final JBLabel navigateIcon = new JBLabel(AllIcons.General.ArrowRight);
    private final JBLabel runIcon = new JBLabel(AllIcons.RunConfigurations.TestState.Run);
    private final JBPanel<?> actionPanel = new JBPanel<>();
    private Color currentRowColor;

    public TestCard() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(CARD_HEIGHT)));

        titleLabel.setFont(JBFont.label().deriveFont(Font.BOLD, UIUtil.getLabelFont().getSize() + 10.0f));
        titleLabel.setForeground(UIUtil.getLabelForeground());
        badgePanel.setOpaque(false);

        // 🌟 1. تجهيز سطر العنوان ليكون أفقياً
        JBPanel<?> titleLine = new JBPanel<>();
        titleLine.setLayout(new BoxLayout(titleLine, BoxLayout.X_AXIS));
        titleLine.setOpaque(false);
        titleLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 🌟 2. تجهيز لوحة الأيقونات
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.X_AXIS));
        actionPanel.setOpaque(false);

        navigateIcon.setToolTipText("Navigate to Code");
        navigateIcon.setOpaque(true);
        navigateIcon.setBorder(JBUI.Borders.empty(4, 6));

        runIcon.setToolTipText("Run test case");
        runIcon.setOpaque(true);
        runIcon.setBorder(JBUI.Borders.empty(4, 6));

        actionPanel.add(navigateIcon);
        actionPanel.add(Box.createRigidArea(new Dimension(8, 0)));
        actionPanel.add(runIcon);
        actionPanel.setVisible(false);

        // 🌟 3. إضافة العنوان، ثم مسافة 10 بكسل، ثم الأيقونات بجانبه مباشرة!
        titleLine.add(titleLabel);
        titleLine.add(Box.createRigidArea(new Dimension(10, 0)));
        titleLine.add(actionPanel);

        badgePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JBPanel<?> content = new JBPanel<>(new VerticalLayout(JBUI.scale(4)));
        content.setOpaque(false);
        content.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(titleLine); // سطر العنوان والأيقونات معاً
        content.add(badgePanel);
        content.add(expectedLabel);
        content.add(stepsLabel);
        content.add(automationRefLabel);
        content.add(businessRefLabel);
        content.add(moduleLabel);
        content.add(idLabel);

        BorderLayoutPanel wrapper = new BorderLayoutPanel();
        wrapper.setOpaque(false);
        wrapper.setBorder(JBUI.Borders.empty(12, 16));
        wrapper.addToCenter(content);

        add(wrapper, BorderLayout.CENTER);
    }

    public void updateData(final int index, final TestCaseDto tc, final boolean showGroups, final boolean showPriority, final Set<String> activeDetails, final boolean isUnsorted) {
        titleLabel.setText((index + 1) + ". " + tc.getTitle());
        expectedLabel.setText("Expected Result: " + tc.getExpected());
        stepsLabel.setText("Steps: " + tc.getSteps());
        automationRefLabel.setText("Automation Reference: " + tc.getAutoRef());
        businessRefLabel.setText("Business Reference: " + tc.getBusiRef());
        moduleLabel.setText("Module: " + tc.getModule());
        idLabel.setText("ID: " + tc.getId());

        currentRowColor = index % 2 == 0 ? new JBColor(Gray._245, Gray._60) : new JBColor(Gray._230, Gray._45);
        setBackground(currentRowColor);
        setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 1, 0));

        expectedLabel.setVisible(activeDetails.contains("Expected Result"));
        stepsLabel.setVisible(activeDetails.contains("Steps"));
        automationRefLabel.setVisible(activeDetails.contains("Automation Ref"));
        businessRefLabel.setVisible(activeDetails.contains("Business Ref"));
        moduleLabel.setVisible(activeDetails.contains("Module"));
        idLabel.setVisible(activeDetails.contains("ID"));

        badgePanel.removeAll();

        if (isUnsorted) {
            JBLabel unsortedBadge = new JBLabel("Unsorted");
            unsortedBadge.setOpaque(true);
            unsortedBadge.setBackground(new JBColor(new Color(255, 200, 200), new Color(130, 50, 50)));
            unsortedBadge.setForeground(JBColor.RED);
            unsortedBadge.setFont(JBUI.Fonts.smallFont().asBold());
            badgePanel.add(unsortedBadge);
        }

        if (showPriority) badgePanel.add(Shared.createPriorityBadge(tc));
        if (showGroups) {
            List<GroupType> groups = tc.getGroups();
            if (groups != null)
                for (GroupType groupName : groups)
                    badgePanel.add(Shared.createGroupBadge(groupName));
        }
        badgePanel.revalidate();
        badgePanel.repaint();
    }

    // 🌟 4. تم تغيير الاسم ليكون منطقياً (إظهار الأيقونات يعتمد على التحديد الآن)
    public void setActionsState(boolean showActions, String hoveredIconName) {
        if (actionPanel.isVisible() != showActions) {
            actionPanel.setVisible(showActions);
        }

        if (showActions) {
            Color hoverColor = ColorUtil.isDark(currentRowColor)
                    ? ColorUtil.brighter(currentRowColor, 2)
                    : ColorUtil.darker(currentRowColor, 1);

            navigateIcon.setBackground("NAVIGATE".equals(hoveredIconName) ? hoverColor : UIUtil.TRANSPARENT_COLOR);
            runIcon.setBackground("RUN".equals(hoveredIconName) ? hoverColor : UIUtil.TRANSPARENT_COLOR);
        } else {
            navigateIcon.setBackground(UIUtil.TRANSPARENT_COLOR);
            runIcon.setBackground(UIUtil.TRANSPARENT_COLOR);
        }
    }

    private JBLabel createDetailLabel() {
        JBLabel label = new JBLabel();
        label.setFont(UIUtil.getLabelFont(UIUtil.FontSize.NORMAL));
        label.setForeground(UIUtil.getContextHelpForeground());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}