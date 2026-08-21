package org.testin.editor.toolbar.components;


import com.intellij.ide.HelpTooltip;
import com.intellij.openapi.application.WriteIntentReadAction;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.util.IconManager;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;

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

    public AbstractButton(final @NotNull String tooltip, final @NotNull Icon icon) {
        super(null, icon);
        // Swing's own contract: a null tooltip is no tooltip at all, and an
        // empty one is a small empty box that follows the pointer.
        setToolTipText(tooltip.isEmpty() ? null : tooltip);
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
        final @NotNull Dimension size = getPreferredSize();
        setIcon(restIcon);

        // Swing cannot build this one. BasicLookAndFeel.getDisabledIcon only grays
        // an ImageIcon and returns null for anything else, and platform icons are
        // not ImageIcons - so without this a disabled button paints its normal icon
        // and looks clickable while ignoring every click.
        //
        // Derived from this button's own icon, so it cannot drift from what the
        // button actually shows.
        setDisabledIcon(IconLoader.getDisabledIcon(restIcon));

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

    /**
     * A button whose command also has a keystroke. The platform's own tooltip
     * draws the description and the shortcut together, the way every IDE
     * toolbar button does, so the key is discoverable without opening the menu
     * that also carries it.
     * <p>
     * The Swing tooltip is deliberately left unset - no tooltip, which is what an
     * empty one means to the constructor above: HelpTooltip replaces it, and a
     * button carrying both shows the plain one on some paths and the rich one on
     * others.
     */
    public AbstractButton(final @NotNull String tooltip, final @NotNull Icon icon, final @NotNull Shortcuts shortcut) {
        this("", icon);

        new HelpTooltip()
                .setDescription(HtmlChunk.text(tooltip))
                .setShortcut(shortcut.getShortcut())
                .installOn(this);
    }

    /**
     * A Swing click arrives without the lock the action system takes before it
     * calls an AnAction. So whatever the click opens runs on the EDT with no
     * read access, and the Create Test Case dialog's editor field asserted on
     * exactly that.
     * <p>
     * Taken here, once, so every toolbar button behaves like the same command
     * reached through the menu or its shortcut.
     */
    // The platform marks WriteIntentReadAction experimental, and it is what the
    // action system itself takes before dispatching - so the alternative is not
    // a stable API, it is doing without the lock and asserting on the EDT.
    @SuppressWarnings("UnstableApiUsage")
    @Override
    protected void fireActionPerformed(final @NotNull ActionEvent event) {
        WriteIntentReadAction.run(() -> super.fireActionPerformed(event));
    }

    private void setHovered(final boolean isHovered) {
        this.hovered = isHovered;
        setIcon(isHovered ? zoomedIcon : restIcon);
        repaint();
    }

    @Override
    protected void paintComponent(final @NotNull Graphics g) {
        final @NotNull Container parent = getParent();
        g.setColor(Optional.ofNullable(parent).map(Container::getBackground).orElseGet(this::getBackground));
        g.fillRect(0, 0, getWidth(), getHeight());

        if (hovered) {
            final @NotNull Graphics2D g2 = (Graphics2D) g.create();
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
