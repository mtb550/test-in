package testGit.editorPanel;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import testGit.pojo.Groups;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;

public class Shared {
    private static final int BADGE_RADIUS = 20;

    private Shared() {
    }

    public static JBLabel createPriorityBadge(TestCaseDto tc) {
        Color bg = tc.getPriority() != null ? tc.getPriority().getColor() : JBColor.GRAY;
        String name = tc.getPriority() != null ? tc.getPriority().name() : "UNKNOWN";

        return new RoundedBadge(name, bg);
    }

    public static JBLabel createGroupBadge(Groups groupName) {
        return new RoundedBadge(groupName.name(), JBColor.darkGray);
    }

    public static void drawTitleActionIcons(Component c, Graphics g, int titleWidth, int y, String hoveredAction) {
        int startX = JBUI.scale(16) + titleWidth + JBUI.scale(10);
        float scaleFactor = 1.5f;

        Icon navIcon = AllIcons.Nodes.Class;
        if ("NAVIGATE" .equals(hoveredAction)) {
            Icon scaledIcon = IconUtil.scale(navIcon, c, scaleFactor);
            int offsetX = (scaledIcon.getIconWidth() - navIcon.getIconWidth()) / 2;
            int offsetY = (scaledIcon.getIconHeight() - navIcon.getIconHeight()) / 2;
            scaledIcon.paintIcon(c, g, startX - offsetX, y - offsetY);

        } else {
            navIcon.paintIcon(c, g, startX, y);
        }

        int runStartX = startX + navIcon.getIconWidth() + JBUI.scale(8);
        Icon runIcon = AllIcons.RunConfigurations.TestState.Run;
        if ("RUN" .equals(hoveredAction)) {
            Icon scaledIcon = IconUtil.scale(runIcon, c, scaleFactor);
            int offsetX = (scaledIcon.getIconWidth() - runIcon.getIconWidth()) / 2;
            int offsetY = (scaledIcon.getIconHeight() - runIcon.getIconHeight()) / 2;
            scaledIcon.paintIcon(c, g, runStartX - offsetX, y - offsetY);

        } else {
            runIcon.paintIcon(c, g, runStartX, y);
        }
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