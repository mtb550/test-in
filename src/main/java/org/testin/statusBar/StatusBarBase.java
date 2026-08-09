package org.testin.statusBar;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public abstract class StatusBarBase {
    final String innerSeparator = ": ";
    final String outerSeparator = ",  ";
    private final JBPanel<?> statusBar;
    private final Color shortcutColor = JBColor.GRAY;
    private final Color dotColor = JBColor.GRAY;
    private final Color labelColor = JBColor.GRAY;
    private final Color separatorColor = JBColor.GRAY;
    private final Font font = JBUI.Fonts.smallFont();
    private final Icon icon = AllIcons.Actions.IntentionBulb;
    private final Border border = JBUI.Borders.emptyRight(4);

    public StatusBarBase(final IStatusBarItem[] items) {
        this.statusBar = new JBPanel<>(new BorderLayout());
        this.statusBar.setBorder(JBUI.Borders.empty(6, 10));
        this.statusBar.setOpaque(true);
        this.statusBar.setBackground(UIUtil.getPanelBackground());

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
                contentPanel.add(createseparator());
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
        label.setFont(font);
        return label;
    }

    private JBLabel createDot() {
        JBLabel label = new JBLabel(innerSeparator);
        label.setForeground(dotColor);
        return label;
    }

    private JBLabel createLabel(final String text) {
        JBLabel label = new JBLabel(text);
        label.setForeground(labelColor);
        label.setFont(font);
        return label;
    }

    private JBLabel createseparator() {
        JBLabel label = new JBLabel(outerSeparator);
        label.setForeground(separatorColor);
        return label;
    }

    public JBPanel<?> getPanel() {
        return statusBar;
    }
}