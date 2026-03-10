package testGit.editorPanel;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import testGit.pojo.GroupType;
import testGit.pojo.TestCase;

import java.awt.*;

/**
 * Shared UI utilities for editor card components.
 * Centralises badge rendering and priority colour logic used across
 * {@code TestCaseCard} and {@code TestRunCard}.
 */
public class Shared {
    private static final int BADGE_RADIUS = 20;

    private Shared() {
    }

    // -------------------------------------------------------------------------
    // Badge factories
    // -------------------------------------------------------------------------

    /**
     * Creates a priority badge for the given test case.
     * Colour is determined by priority level; corners are rounded by {@code radius}.
     *
     * @param tc the test case whose priority is displayed
     */
    public static JBLabel createPriorityBadge(TestCase tc) {
        Color bg = switch (tc.getPriority()) {
            case HIGH -> JBColor.CYAN;
            case MEDIUM -> JBColor.magenta;
            case LOW -> JBColor.GRAY;
        };
        return new RoundedBadge(tc.getPriority().getDescription(), bg);
    }

    /**
     * Creates a group badge for the given group type.
     *
     * @param groupName the group to label
     */
    public static JBLabel createGroupBadge(GroupType groupName) {
        return new RoundedBadge(groupName.name(), JBColor.darkGray);
    }

    // -------------------------------------------------------------------------
    // RoundedBadge
    // -------------------------------------------------------------------------

    /**
     * A small pill-shaped label used for priority and group badges on cards.
     * When {@code radius} is 0 the badge renders with square corners (no custom paint overhead).
     */
    public static class RoundedBadge extends JBLabel {

        private final int radius;

        public RoundedBadge(String text, Color bg) {
            super(text);
            this.radius = BADGE_RADIUS;
            setOpaque(radius == 0); // flat badges can use the default opaque fill
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
