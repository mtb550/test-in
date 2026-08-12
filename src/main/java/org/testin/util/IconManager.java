package org.testin.util;

import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class IconManager {
    private static final float SCALE_FACTOR = 1.3f;
    private static final float DEFAULT_FACTOR = 1.0f;

    public static @NotNull Icon createIcon(final @NotNull Color color) {
        return createScaledIcon(color, DEFAULT_FACTOR);
    }

    public static @NotNull Icon zoomStandardIcon(final @NotNull Icon icon, final @NotNull Component contextComponent) {
        return IconUtil.scale(icon, contextComponent, SCALE_FACTOR);
    }

    private static @NotNull Icon createScaledIcon(final @NotNull Color color, final float scale) {
        return new Icon() {
            @Override
            public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
                final Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(color);

                    final int size = JBUI.scale((int) (10 * scale));
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