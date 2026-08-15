package org.testin.editor.toolBar.components;

import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.util.IconManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A flat toolbar button: the icon alone at rest, a rounded hover pill and a
 * slightly larger icon under the pointer.
 * <p>
 * It paints its own background rather than leaving it to the look and feel. A
 * JButton is opaque, which promises Swing that every pixel inside the bounds is
 * covered, so a repaint of the button alone never repaints the toolbar behind
 * it. The stock painting does not keep that promise here - with the content
 * area unfilled it draws an icon and leaves the rest untouched, so those pixels
 * keep whatever was drawn there before. Filling from the toolbar's own color
 * keeps the promise and keeps the button invisible at rest.
 */
public abstract class AbstractButton extends JButton {

    private final @NotNull Icon restIcon;
    private final @NotNull Icon zoomedIcon;

    private boolean hovered;

    public AbstractButton(final @Nullable String tooltip, final @NotNull Icon icon) {
        super(null, icon);
        setToolTipText(tooltip);
        setFocusable(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Every pixel inside the bounds is still covered - paintComponent fills
        // them - but it has to be this class that does it, not the look and feel.
        // While opaque, the UI fills the whole rectangle with Button.background
        // before anything else is drawn, which is the wrong color here and
        // erases the hover pill along with it.
        setOpaque(false);

        this.restIcon = icon;
        this.zoomedIcon = IconManager.zoomStandardIcon(icon, this);

        // Measured with the zoomed icon, then frozen. The look and feel keeps
        // deciding the spacing, so the toolbar looks as it always has; freezing
        // it stops setIcon() - which revalidates - from growing the button when
        // the pointer arrives and shrinking it when it leaves, which re-laid out
        // the whole toolbar and shifted the buttons under the pointer.
        setIcon(zoomedIcon);
        final Dimension size = getPreferredSize();
        setIcon(icon);

        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                setHovered(true);
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                setHovered(false);
            }
        });
    }

    private void setHovered(final boolean isHovered) {
        this.hovered = isHovered;
        setIcon(isHovered ? zoomedIcon : restIcon);
        repaint();
    }

    @Override
    protected void paintComponent(final @NotNull Graphics g) {
        final Container parent = getParent();
        g.setColor(parent != null ? parent.getBackground() : getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        if (hovered) {
            final Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(JBUI.CurrentTheme.ActionButton.hoverBackground());
                final int arc = JBUI.scale(6);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            } finally {
                g2.dispose();
            }
        }

        super.paintComponent(g);
    }
}
