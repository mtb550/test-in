package org.testin.editorPanel;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.util.FontSync;

import java.awt.*;

/**
 * Rounded pill label used for priority and group badges on the cards.
 */
public class RoundedBadge extends JBLabel {

    private static final int BADGE_RADIUS = 20;

    private final int radius;

    public RoundedBadge(final @NotNull String text, final @NotNull Color bg) {
        super(text);
        this.radius = BADGE_RADIUS;
        setOpaque(false);
        setBackground(bg);
        setForeground(JBColor.WHITE);

        final float badgeSize = Math.max(8.0f, FontSync.getBaseFontSize() - 2.0f);
        setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL).deriveFont(Font.BOLD, badgeSize));

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
