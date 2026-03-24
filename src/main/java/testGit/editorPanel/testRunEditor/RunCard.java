package testGit.editorPanel.testRunEditor;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import testGit.editorPanel.BaseCard;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class RunCard extends BaseCard<RunCard> {

    public static final int ACTIONS_TOTAL_WIDTH = 210;

    private final JBPanel<?> actionPanel = new JBPanel<>();
    private final JBLabel passedBtn = createActionLabel("PASSED");
    private final JBLabel failedBtn = createActionLabel("FAILED");
    private final JBLabel blockedBtn = createActionLabel("BLOCKED");

    public RunCard() {
        super();
        actionPanel.setLayout(new GridLayout(1, 3, 0, 0));
        actionPanel.setOpaque(false);
        actionPanel.setPreferredSize(new Dimension(JBUI.scale(ACTIONS_TOTAL_WIDTH), 0));

        actionPanel.add(passedBtn);
        actionPanel.add(failedBtn);
        actionPanel.add(blockedBtn);

        actionPanel.setVisible(false);
        actionPanel.setBorder(JBUI.Borders.empty());

        this.add(actionPanel, BorderLayout.EAST);
    }

    public void updateData(int index, TestCaseDto tc, boolean showGroups, boolean showPriority, Set<String> activeDetails) {
        super.updateBaseData(index, tc, showPriority, showGroups, activeDetails);

        badgePanel.revalidate();
        badgePanel.repaint();
    }

    @Override
    public void setActionsState(boolean isSelected, boolean isRowHovered, String hoveredAction) {
        super.setActionsState(isSelected, isRowHovered, hoveredAction);

        if (actionPanel.isVisible() != isSelected) {
            actionPanel.setVisible(isSelected);
        }

        passedBtn.setOpaque(false);
        failedBtn.setOpaque(false);
        blockedBtn.setOpaque(false);

        passedBtn.setForeground(JBColor.GRAY);
        failedBtn.setForeground(JBColor.GRAY);
        blockedBtn.setForeground(JBColor.GRAY);

        if ("PASSED".equals(hoveredAction)) {
            passedBtn.setOpaque(true);
            passedBtn.setBackground(new JBColor(new Color(39, 174, 96, 40), new Color(46, 125, 50, 60)));
            passedBtn.setForeground(new JBColor(new Color(39, 174, 96), new Color(129, 199, 132)));

        } else if ("FAILED".equals(hoveredAction)) {
            failedBtn.setOpaque(true);
            failedBtn.setBackground(new JBColor(new Color(192, 57, 43, 40), new Color(183, 28, 28, 60)));
            failedBtn.setForeground(new JBColor(new Color(192, 57, 43), new Color(229, 115, 115)));

        } else if ("BLOCKED".equals(hoveredAction)) {
            blockedBtn.setOpaque(true);
            blockedBtn.setBackground(new JBColor(new Color(243, 156, 18, 40), new Color(237, 108, 2, 60)));
            blockedBtn.setForeground(new JBColor(new Color(243, 156, 18), new Color(255, 183, 77)));
        }
    }

    private JBLabel createActionLabel(String text) {
        JBLabel lbl = new JBLabel(text, SwingConstants.CENTER);
        lbl.setOpaque(false);
        lbl.setFont(JBFont.regular().asBold());
        lbl.setBorder(JBUI.Borders.empty());
        return lbl;
    }
}