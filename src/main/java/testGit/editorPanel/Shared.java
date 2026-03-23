package testGit.editorPanel;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import testGit.pojo.GroupType;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;

public class Shared {
    private static final int BADGE_RADIUS = 20;

    private Shared() {
    }

    public static JBLabel createPriorityBadge(TestCaseDto tc) {
        Color bg = switch (tc.getPriority()) {
            case HIGH -> JBColor.CYAN;
            case MEDIUM -> JBColor.magenta;
            case LOW -> JBColor.GRAY;
        };
        return new RoundedBadge(tc.getPriority().getDescription(), bg);
    }

    public static JBLabel createGroupBadge(GroupType groupName) {
        return new RoundedBadge(groupName.name(), JBColor.darkGray);
    }

    public static void drawTitleActionIcons(Component c, Graphics g, int titleWidth, int y) {
        int startX = 16 + titleWidth + 10;

        Icon navIcon = AllIcons.General.ArrowRight;
        navIcon.paintIcon(c, g, startX, y);

        int runStartX = startX + 28 + 8;
        Icon runIcon = AllIcons.RunConfigurations.TestState.Run;
        runIcon.paintIcon(c, g, runStartX, y);
    }

    public static class RoundedBadge extends JBLabel {
        private final int radius;

        public RoundedBadge(String text, Color bg) {
            super(text);
            this.radius = BADGE_RADIUS;
            setOpaque(false);
            setBackground(bg);
            setForeground(JBColor.WHITE);
            setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL).deriveFont(Font.BOLD));
            setBorder(JBUI.Borders.empty(2, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (radius > 0) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}