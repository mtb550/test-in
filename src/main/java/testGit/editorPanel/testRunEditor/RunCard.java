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

    private final JBPanel<?> actionPanel = new JBPanel<>();
    private final JBLabel passedBtn = createActionLabel("PASSED");
    private final JBLabel failedBtn = createActionLabel("FAILED");
    private final JBLabel blockedBtn = createActionLabel("BLOCKED");

    public RunCard() {
        super();
        actionPanel.setLayout(new GridLayout(3, 1, 0, JBUI.scale(4)));
        actionPanel.setOpaque(false);
        actionPanel.setPreferredSize(new Dimension(JBUI.scale(90), 0));
        actionPanel.add(passedBtn);
        actionPanel.add(failedBtn);
        actionPanel.add(blockedBtn);
        actionPanel.setVisible(false);
        actionPanel.setBorder(JBUI.Borders.empty(2, 0));

        wrapper.addToRight(actionPanel);
    }

    public void updateData(int index, TestCaseDto tc, boolean showGroups, boolean showPriority, Set<String> activeDetails) {
        super.updateBaseData(index, tc, showPriority, showGroups, activeDetails);

        badgePanel.revalidate();
        badgePanel.repaint();
    }

    @Override
    public void setActionsState(boolean isSelected) {
        super.setActionsState(isSelected);

        if (actionPanel.isVisible() != isSelected) {
            actionPanel.setVisible(isSelected);
        }

        passedBtn.setBackground(new JBColor(new Color(39, 174, 96, 150), new Color(46, 125, 50, 150)));
        failedBtn.setBackground(new JBColor(new Color(192, 57, 43, 150), new Color(183, 28, 28, 150)));
        blockedBtn.setBackground(new JBColor(new Color(243, 156, 18, 150), new Color(237, 108, 2, 150)));
    }

    private JBLabel createActionLabel(String text) {
        JBLabel lbl = new JBLabel(text, SwingConstants.CENTER);
        lbl.setOpaque(true);
        lbl.setFont(JBFont.regular().asBold());
        lbl.setForeground(JBColor.WHITE);
        lbl.setBorder(JBUI.Borders.empty());
        return lbl;
    }
}