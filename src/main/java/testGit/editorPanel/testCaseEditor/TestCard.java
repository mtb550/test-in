package testGit.editorPanel.testCaseEditor;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import testGit.editorPanel.BaseCard;
import testGit.pojo.dto.TestCaseDto;

import java.awt.*;
import java.util.Set;

public class TestCard extends BaseCard<TestCard> {

    public TestCard() {
        super();
    }

    public void updateData(final int index, final TestCaseDto tc, final boolean showGroups, final boolean showPriority, final Set<String> activeDetails, final boolean isUnsorted) {
        super.updateBaseData(index, tc, showPriority, showGroups, activeDetails);

        if (isUnsorted) {
            JBLabel unsortedBadge = new JBLabel("Unsorted");
            unsortedBadge.setOpaque(true);
            unsortedBadge.setBackground(new JBColor(new Color(255, 200, 200), new Color(130, 50, 50)));
            unsortedBadge.setForeground(JBColor.RED);
            unsortedBadge.setFont(JBUI.Fonts.smallFont().asBold());
            badgePanel.add(unsortedBadge);
        }

        badgePanel.revalidate();
        badgePanel.repaint();
    }
}