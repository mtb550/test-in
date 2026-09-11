package org.testin.util;

import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * The two icons Testin makes for itself: a stock one enlarged, and a colored
 * dot.
 * <p>
 * Was {@code IconManager}, which managed nothing and is also the name of
 * {@code com.intellij.ui.IconManager} in the platform - the same collision
 * {@code EditorUtil} had, and the reason CLAUDE.md asks for a name that says
 * what the class does (#291).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Icons {
    private static final float SCALE_FACTOR = 1.3f;
    private static final int DOT_SIZE = 10;

    public static @NotNull Icon zoomStandardIcon(final @NotNull Icon icon, final @NotNull Component contextComponent) {
        return IconUtil.scale(icon, contextComponent, SCALE_FACTOR);
    }

    /**
     * The colored dot the priority and group rows are marked with, centered in a
     * standard 16px icon.
     */
    public static @NotNull Icon dot(final @NotNull Color color) {
        return new Icon() {
            @Override
            public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
                final @NotNull Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(color);

                    final int size = JBUI.scale(DOT_SIZE);
                    final int totalWidth = getIconWidth();
                    final int totalHeight = getIconHeight();
                    final int centeredX = x + (totalWidth - size) / 2;
                    final int centeredY = y + (totalHeight - size) / 2;
                    g2.fillOval(centeredX, centeredY, size, size);
                } finally {
                    g2.dispose();
                }
            }

            @Override
            public int getIconWidth() {
                return JBUI.scale(16);
            }

            @Override
            public int getIconHeight() {
                return JBUI.scale(16);
            }
        };
    }
}