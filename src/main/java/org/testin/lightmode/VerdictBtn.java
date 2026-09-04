package org.testin.lightmode;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestStatus;
import org.testin.ui.framework.Keycap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * One of light mode's three verdicts: the key that gives it, and the word for
 * what it means (#13).
 * <p>
 * <b>All three are drawn the same.</b> A verdict color says a case has been
 * judged; a button offering to judge one has not, so painting Passed green and
 * Failed red here would put the answer on screen before the tester gave it.
 * Color returns on the failure form, where FAILED has been chosen.
 * <p>
 * A panel rather than a button, because a button draws one string and this is a
 * keycap beside a word - and the keycap is {@link Keycap}, the same one the
 * status bar of every dialog uses.
 */
class VerdictBtn extends JBPanel<VerdictBtn> {

    private final @NotNull Runnable onChosen;

    private boolean hovered;

    VerdictBtn(final @NotNull TestStatus status, final @NotNull Runnable onChosen) {
        super(new FlowLayout(FlowLayout.CENTER, JBUI.scale(6), JBUI.scale(5)));
        this.onChosen = onChosen;

        setToolTipText("Record " + status.getLabel().toLowerCase() + " for this test case");
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(JBUI.Borders.customLine(JBColor.border(), 1));
        setOpaque(false);

        add(Keycap.of(keyOf(status)));
        add(new JBLabel(status.getLabel()));

        listen();
    }

    /**
     * The letter the tester presses, taken from the status itself - the same
     * declaration that binds the key in {@link LightModeWindow}, so the cap can
     * never name a key that does nothing.
     */
    static @NotNull String keyOf(final @NotNull TestStatus status) {
        return String.valueOf((char) status.getMenuEntry().shortcut().getKeyCode());
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
                onChosen.run();
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
