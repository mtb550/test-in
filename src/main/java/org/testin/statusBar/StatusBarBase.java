package org.testin.statusBar;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;

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
    private static final String INNER_SEPARATOR = " ";
    /**
     * Between entries — whitespace only, sized to read as a deliberate gap.
     */
    private static final String OUTER_SEPARATOR = "       ";

    private final JBPanel<?> statusBar;

    // Keystroke and its meaning read clearly in light and dark; only the
    // separators stay muted.
    private final Color shortcutColor = JBUI.CurrentTheme.Label.foreground();
    private final Color labelColor = JBUI.CurrentTheme.Label.foreground();
    private final Color dotColor = JBUI.CurrentTheme.ContextHelp.FOREGROUND;
    private final Color separatorColor = JBUI.CurrentTheme.ContextHelp.FOREGROUND;

    private final Font font = JBUI.Fonts.smallFont();
    private final Font shortcutFont = JBUI.Fonts.smallFont().asBold();

    // A keyboard: says "these are keys".
    private final Icon icon = AllIcons.General.Keyboard;
    private final Border border = JBUI.Borders.emptyRight(6);

    public StatusBarBase(final IStatusBarItem[] items) {
        this.statusBar = new JBPanel<>(new BorderLayout());
        this.statusBar.setBorder(JBUI.Borders.empty(4, 10));
        this.statusBar.setOpaque(true);
        this.statusBar.setBackground(JBUI.CurrentTheme.Advertiser.background());

        updateItems(items);
    }

    public void updateItems(final IStatusBarItem[] items) {
        this.statusBar.removeAll();

        JBPanel<?> contentPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        contentPanel.setOpaque(false);

        contentPanel.add(setStatusBarIcon());

        for (int i = 0; i < items.length; i++) {
            IStatusBarItem item = items[i];
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

    private JBLabel setStatusBarIcon() {
        JBLabel label = new JBLabel(icon);
        label.setBorder(border);
        return label;
    }

    private JBLabel createShortcut(final String text) {
        JBLabel label = new JBLabel(text);
        label.setForeground(shortcutColor);
        label.setFont(shortcutFont);
        return label;
    }

    private JBLabel createDot() {
        JBLabel label = new JBLabel(INNER_SEPARATOR);
        label.setForeground(dotColor);
        return label;
    }

    private JBLabel createLabel(final String text) {
        JBLabel label = new JBLabel(text);
        label.setForeground(labelColor);
        label.setFont(font);
        return label;
    }

    private JBLabel createSeparator() {
        JBLabel label = new JBLabel(OUTER_SEPARATOR);
        label.setForeground(separatorColor);
        label.setFont(font);
        return label;
    }

    public JBPanel<?> getPanel() {
        return statusBar;
    }
}
