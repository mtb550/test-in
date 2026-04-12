package testGit.pojo;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import testGit.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;

@Getter
public enum Priority {
    HIGH("High", JBColor.RED.brighter().brighter(), true, KeyboardSet.PriorityHigh),
    MEDIUM("Medium", JBColor.BLUE.brighter(), true, KeyboardSet.PriorityMedium),
    LOW("Low", JBColor.GRAY.brighter(), true, KeyboardSet.PriorityLow);

    private final String displayName;
    private final Color color;
    private final boolean active;
    private final KeyboardSet shortcut;

    private Icon cachedIcon;

    Priority(final String displayName, final Color color, final boolean active, final KeyboardSet shortcut) {
        this.displayName = displayName;
        this.color = color;
        this.active = active;
        this.shortcut = shortcut;
    }

    public String getShortcutText() {
        return shortcut.getShortcutText();
    }

    public Icon getIcon() {
        if (cachedIcon == null) {
            cachedIcon = new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g2.setColor(color);
                    int size = JBUI.scale(10);
                    int yOffset = y + (getIconHeight() - size) / 2;

                    g2.fillOval(x + JBUI.scale(2), yOffset, size, size);
                    g2.dispose();
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
        return cachedIcon;
    }
}