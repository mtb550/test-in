package testGit.editorPanel;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import testGit.pojo.CardHoverAction;
import testGit.pojo.Groups;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public class Shared {
    private static final int BADGE_RADIUS = 20;

    public static JBLabel createPriorityBadge(final TestCaseDto tc) {
        return Optional.ofNullable(tc.getPriority())
                .map(p -> new RoundedBadge(p.getDisplayName(), p.getColor()))
                .orElseGet(() -> new RoundedBadge("Unknown", JBColor.GRAY));
    }

    public static JBLabel createGroupBadge(final Groups groupName) {
        return new RoundedBadge(groupName.getDisplayName(), JBColor.darkGray);
    }

    public static void drawTitleActionIcons(final Component c, final Graphics g, final int titleWidth, final int y, final String hoveredAction) {
        final int startX = JBUI.scale(16) + titleWidth + JBUI.scale(10);
        final float scaleFactor = 1.5f;

        final Icon navIcon = AllIcons.Nodes.Class;
        if (CardHoverAction.NAVIGATE.name().equals(hoveredAction)) {
            final Icon scaledIcon = IconUtil.scale(navIcon, c, scaleFactor);
            final int offsetX = (scaledIcon.getIconWidth() - navIcon.getIconWidth()) / 2;
            final int offsetY = (scaledIcon.getIconHeight() - navIcon.getIconHeight()) / 2;
            scaledIcon.paintIcon(c, g, startX - offsetX, y - offsetY);
        } else {
            navIcon.paintIcon(c, g, startX, y);
        }

        final int runStartX = startX + navIcon.getIconWidth() + JBUI.scale(8);
        final Icon runIcon = AllIcons.RunConfigurations.TestState.Run;
        if (CardHoverAction.RUN.name().equals(hoveredAction)) {
            final Icon scaledIcon = IconUtil.scale(runIcon, c, scaleFactor);
            final int offsetX = (scaledIcon.getIconWidth() - runIcon.getIconWidth()) / 2;
            final int offsetY = (scaledIcon.getIconHeight() - runIcon.getIconHeight()) / 2;
            scaledIcon.paintIcon(c, g, runStartX - offsetX, y - offsetY);
        } else {
            runIcon.paintIcon(c, g, runStartX, y);
        }
    }

    public static class RoundedBadge extends JBLabel {
        private final int radius;

        public RoundedBadge(final String text, final Color bg) {
            super(text);
            this.radius = BADGE_RADIUS;
            setOpaque(false);
            setBackground(bg);
            setForeground(JBColor.WHITE);
            setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL).deriveFont(Font.BOLD));
            setBorder(JBUI.Borders.empty(2, 10));
        }

        @Override
        protected void paintComponent(final Graphics g) {
            if (radius > 0) {
                final Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}