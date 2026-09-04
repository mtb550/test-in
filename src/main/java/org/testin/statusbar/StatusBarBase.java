package org.testin.statusbar;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Shortcut-hint strip at the bottom of the dialogs, styled like the
 * platform's popup advertiser bar: tinted background, hairline on top, muted
 * hint text with the keystroke emphasized.
 */
public abstract class StatusBarBase {
    /**
     * Between a keystroke and its meaning.
     */
    private static final @NotNull String INNER_SEPARATOR = " ";
    /**
     * Between entries — whitespace only, sized to read as a deliberate gap.
     */
    private static final @NotNull String OUTER_SEPARATOR = "       ";

    private final @NotNull JBPanel<?> statusBar;

    // Keystroke and its meaning read clearly in light and dark; only the
    // separators stay muted.
    private final @NotNull Color shortcutColor = JBUI.CurrentTheme.Label.foreground();
    private final @NotNull Color labelColor = JBUI.CurrentTheme.Label.foreground();
    private final @NotNull Color dotColor = JBUI.CurrentTheme.ContextHelp.FOREGROUND;
    private final @NotNull Color separatorColor = JBUI.CurrentTheme.ContextHelp.FOREGROUND;

    private final @NotNull Font font = JBUI.Fonts.smallFont();
    private final @NotNull Font shortcutFont = JBUI.Fonts.smallFont().asBold();

    // A keystroke is drawn as a key. The heavier bottom edge is the whole
    // trick: one pixel on three sides and two underneath reads as a keycap,
    // where an even border reads as a box around some text.
    private final @NotNull Color keyBorder = JBColor.border();
    private final @NotNull Color keyBackground = UIUtil.getTextFieldBackground();

    // A keyboard: says "these are keys".
    private final @NotNull Icon icon = AllIcons.General.Keyboard;
    private final @NotNull Border border = JBUI.Borders.emptyRight(6);

    public StatusBarBase(final StatusBarItem @NotNull [] items) {
        this.statusBar = new JBPanel<>(new BorderLayout());
        this.statusBar.setBorder(JBUI.Borders.empty(4, 10));
        this.statusBar.setOpaque(true);
        this.statusBar.setBackground(JBUI.CurrentTheme.Advertiser.background());

        updateItems(items);
    }

    public void updateItems(final StatusBarItem @NotNull [] items) {
        this.statusBar.removeAll();

        final @NotNull JBPanel<?> contentPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        contentPanel.setOpaque(false);

        contentPanel.add(setStatusBarIcon());

        for (int i = 0; i < items.length; i++) {
            final @NotNull StatusBarItem item = items[i];
            contentPanel.add(createShortcut(item.getShortcutText()));
            contentPanel.add(createDot());
            contentPanel.add(createLabel(item.getName()));

            if (i < items.length - 1) {
                contentPanel.add(createSeparator());
            }
        }

        this.statusBar.add(contentPanel, BorderLayout.WEST);

        this.statusBar.revalidate();
        this.statusBar.repaint();
    }

    private @NotNull JBLabel setStatusBarIcon() {
        final @NotNull JBLabel label = new JBLabel(icon);
        label.setBorder(border);
        return label;
    }

    /**
     * The keystroke, drawn as a key rather than printed as text.
     * <p>
     * Here rather than in the window that wanted it: every framework dialog
     * shows its shortcuts through this one method, so the keys look like keys in
     * all twenty-five of them or in none. The light mode window's status bar
     * (#13) asked for the treatment; the framework is where the treatment lives.
     * <p>
     * A blank keystroke is left as bare text. Some status bar items carry no key
     * of their own - a create-dialog field with no letter - and an empty keycap
     * is a box drawn around nothing.
     */
    private @NotNull JBLabel createShortcut(final @NotNull String text) {
        final @NotNull JBLabel label = new JBLabel(text);
        label.setForeground(shortcutColor);
        label.setFont(shortcutFont);

        if (text.isBlank()) return label;

        label.setOpaque(true);
        label.setBackground(keyBackground);
        label.setBorder(BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(keyBorder, 1, 1, 2, 1),
                JBUI.Borders.empty(0, 5)));

        return label;
    }

    private @NotNull JBLabel createDot() {
        final @NotNull JBLabel label = new JBLabel(INNER_SEPARATOR);
        label.setForeground(dotColor);
        return label;
    }

    private @NotNull JBLabel createLabel(final @NotNull String text) {
        final @NotNull JBLabel label = new JBLabel(text);
        label.setForeground(labelColor);
        label.setFont(font);
        return label;
    }

    private @NotNull JBLabel createSeparator() {
        final @NotNull JBLabel label = new JBLabel(OUTER_SEPARATOR);
        label.setForeground(separatorColor);
        label.setFont(font);
        return label;
    }

    public @NotNull JBPanel<?> getPanel() {
        return statusBar;
    }
}
