package testGit.editorPanel.testCaseEditor;

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
import testGit.pojo.TestCase;

import java.awt.*;
import java.util.List;
import java.util.Set;

public class TestCaseCard extends JBPanel<TestCaseCard> {
    private static final int CARD_HEIGHT = 130;
    private final JBLabel titleLabel = new JBLabel();
    private final JBPanel<?> badgePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(10), 0));
    private final JBLabel expectedLabel = createDetailLabel();
    private final JBLabel stepsLabel = createDetailLabel();
    private final JBLabel automationRefLabel = createDetailLabel();
    private final JBLabel businessRefLabel = createDetailLabel();
    private final JBLabel moduleLabel = createDetailLabel();
    private final JBLabel idLabel = createDetailLabel();

    public TestCaseCard() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(CARD_HEIGHT)));

        titleLabel.setFont(JBFont.label().deriveFont(Font.BOLD, UIUtil.getLabelFont().getSize() + 10.0f));
        titleLabel.setForeground(UIUtil.getLabelForeground());
        badgePanel.setOpaque(false);

        JBPanel<?> titleLine = new JBPanel<>(new BorderLayout());
        titleLine.setOpaque(false);
        titleLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLine.add(titleLabel, BorderLayout.WEST);
        badgePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JBPanel<?> content = new JBPanel<>(new VerticalLayout(JBUI.scale(4)));
        content.setOpaque(false);
        content.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(titleLine);
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

    public void updateData(int index, TestCase tc, boolean showGroups, boolean showPriority, Set<String> activeDetails) {
        titleLabel.setText((index + 1) + ". " + tc.getTitle());
        expectedLabel.setText("Expected Result: " + tc.getExpected());
        stepsLabel.setText("Steps: " + tc.getSteps());
        automationRefLabel.setText("Automation Reference: " + tc.getAutoRef());
        businessRefLabel.setText("Business Reference: " + tc.getBusiRef());
        moduleLabel.setText("Module: " + tc.getModule());
        idLabel.setText("ID: " + tc.getId());

        setBackground(index % 2 == 0 ? new JBColor(Gray._245, Gray._60) : new JBColor(Gray._230, Gray._45));
        setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 1, 0));

        expectedLabel.setVisible(activeDetails.contains("Expected Result"));
        stepsLabel.setVisible(activeDetails.contains("Steps"));
        automationRefLabel.setVisible(activeDetails.contains("Automation Ref"));
        businessRefLabel.setVisible(activeDetails.contains("Business Ref"));
        moduleLabel.setVisible(activeDetails.contains("Module"));
        idLabel.setVisible(activeDetails.contains("ID"));

        badgePanel.removeAll();
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

    private JBLabel createDetailLabel() {
        JBLabel label = new JBLabel();
        label.setFont(UIUtil.getLabelFont(UIUtil.FontSize.NORMAL));
        label.setForeground(UIUtil.getContextHelpForeground());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}
