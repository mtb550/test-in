package org.testin.lightmode;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.Keycap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A key and what it does, offered as something to click as well (#13).
 * <p>
 * A keycap beside a word: the three verdicts, and nothing else. It exists so
 * the tester can reach with the mouse what they would otherwise reach with the
 * key, which is why the cap on it is the key that works.
 * <p>
 * <b>No color.</b> A verdict color says a case has been judged; a button
 * offering to judge one has not, so painting Passed green here would put the
 * answer on screen before the tester gave it.
 * <p>
 * A panel rather than a button, because a button draws one string and this is a
 * keycap beside a word - and the keycap is {@link Keycap}, the same one the
 * status bar of every dialog uses.
 */
class KeyBtn extends JBPanel<KeyBtn> {

    private final @NotNull Runnable onClick;

    private boolean hovered;

    KeyBtn(final @NotNull String key, final @NotNull String text, final @NotNull String tooltip, final @NotNull Runnable onClick) {
        super(new FlowLayout(FlowLayout.CENTER, JBUI.scale(6), JBUI.scale(5)));
        this.onClick = onClick;

        setToolTipText(tooltip);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(JBUI.Borders.customLine(JBColor.border(), 1));
        setOpaque(false);

        add(Keycap.of(key));
        add(new JBLabel(text));

        listen();
    }

    private void listen() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final @NotNull MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(final @NotNull MouseEvent e) {
                hovered = false;
                repaint();
            }

            @Override
            public void mouseClicked(final @NotNull MouseEvent e) {
                onClick.run();
            }
        });
    }

    @Override
    protected void paintComponent(final @NotNull Graphics g) {
        super.paintComponent(g);

        if (!hovered) return;

        final @NotNull Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(JBUI.CurrentTheme.ActionButton.hoverBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
        } finally {
            g2.dispose();
        }
    }
}
