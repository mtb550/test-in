package testGit.editorPanel;

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
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public abstract class BaseCard<T extends JBPanel<T>> extends JBPanel<T> {
    protected static final int CARD_HEIGHT = 130;

    protected final JBLabel titleLabel = new JBLabel();
    protected final JBPanel<?> badgePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(10), 0));
    protected final JBLabel expectedLabel = createDetailLabel();
    protected final JBLabel stepsLabel = createDetailLabel();
    protected final JBLabel automationRefLabel = createDetailLabel();
    protected final JBLabel businessRefLabel = createDetailLabel();
    protected final JBLabel moduleLabel = createDetailLabel();
    protected final JBLabel idLabel = createDetailLabel();
    protected final JBPanel<?> content = new JBPanel<>(new VerticalLayout(JBUI.scale(4)));
    protected final BorderLayoutPanel wrapper = new BorderLayoutPanel();
    protected boolean isSelected;

    public BaseCard() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(CARD_HEIGHT)));

        titleLabel.setFont(JBFont.label().deriveFont(Font.BOLD, UIUtil.getLabelFont().getSize() + 10.0f));
        titleLabel.setForeground(UIUtil.getLabelForeground());
        badgePanel.setOpaque(false);

        JBPanel<?> titleLine = new JBPanel<>();
        titleLine.setLayout(new BoxLayout(titleLine, BoxLayout.X_AXIS));
        titleLine.setOpaque(false);
        titleLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLine.add(titleLabel);

        badgePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

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

        wrapper.setOpaque(false);
        wrapper.setBorder(JBUI.Borders.empty(12, 16));
        wrapper.addToCenter(content);
        add(wrapper, BorderLayout.CENTER);
    }

    protected void updateBaseData(int index, final TestCaseDto tc, final boolean showPriority, final boolean showGroups, final Set<String> activeDetails) {
        titleLabel.setText(String.format("%d. %s", index + 1, tc.getTitle()));
        expectedLabel.setText("Expected Result: " + tc.getExpected());
        stepsLabel.setText("Steps: " + tc.getSteps());
        automationRefLabel.setText("Automation Reference: " + tc.getAutoRef());
        businessRefLabel.setText("Business Reference: " + tc.getBusiRef());
        moduleLabel.setText("Module: " + tc.getModule());
        idLabel.setText("ID: " + tc.getId());

        Color currentRowColor = index % 2 == 0 ? new JBColor(Gray._245, Gray._60) : new JBColor(Gray._230, Gray._45);
        setBackground(currentRowColor);
        setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 1, 0));

        expectedLabel.setVisible(activeDetails.contains("Expected Result"));
        stepsLabel.setVisible(activeDetails.contains("Steps"));
        automationRefLabel.setVisible(activeDetails.contains("Automation Ref"));
        businessRefLabel.setVisible(activeDetails.contains("Business Ref"));
        moduleLabel.setVisible(activeDetails.contains("Module"));
        idLabel.setVisible(activeDetails.contains("ID"));

        badgePanel.removeAll();

        if (showPriority) badgePanel.add(Shared.createPriorityBadge(tc));

        if (showGroups && tc.getGroups() != null) {
            for (GroupType groupName : tc.getGroups()) {
                badgePanel.add(Shared.createGroupBadge(groupName));
            }
        }
    }

    public void setActionsState(boolean isSelected) {
        this.isSelected = isSelected;
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (isSelected) {
            FontMetrics fm = titleLabel.getFontMetrics(titleLabel.getFont());
            int titleWidth = fm.stringWidth(titleLabel.getText());
            Shared.drawTitleActionIcons(this, g, titleWidth, JBUI.scale(12));
        }
    }

    protected JBLabel createDetailLabel() {
        JBLabel label = new JBLabel();
        label.setFont(UIUtil.getLabelFont(UIUtil.FontSize.NORMAL));
        label.setForeground(UIUtil.getContextHelpForeground());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}