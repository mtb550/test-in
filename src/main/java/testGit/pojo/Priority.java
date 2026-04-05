package testGit.pojo;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
@AllArgsConstructor
public enum Priority { ///  to be implemented, filter by active
    HIGH(JBColor.RED.brighter().brighter(), true),
    MEDIUM(JBColor.BLUE.brighter(), true),
    LOW(JBColor.GRAY.brighter(), true);

    private final Color color;
    private final boolean active;


    public Icon getIcon() {
        return new Icon() {
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
}