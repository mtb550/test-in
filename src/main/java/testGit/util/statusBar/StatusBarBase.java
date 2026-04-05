package testGit.util.statusBar;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public abstract class StatusBarBase {
    final String innerSeparator = " : ";
    final String outerSeparator = "   ";
    private final JPanel statusBar;
    private final Color shortcutColor = JBColor.GRAY;
    private final Color dotColor = JBColor.GRAY;
    private final Color labelColor = JBColor.GRAY;
    private final Color separatorColor = JBColor.GRAY;
    private final Font font = JBUI.Fonts.smallFont();
    private final Icon icon = AllIcons.Actions.IntentionBulb;
    private final Border border = JBUI.Borders.emptyRight(4);

    public StatusBarBase(final StatusBarItem[] items) {
        this.statusBar = new JPanel(new BorderLayout());
        this.statusBar.setBorder(JBUI.Borders.empty(6, 10));
        this.statusBar.setOpaque(true);
        this.statusBar.setBackground(UIUtil.getPanelBackground());

        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        contentPanel.setOpaque(false);

        contentPanel.add(setStatusBarIcon());

        for (int i = 0; i < items.length; i++) {
            StatusBarItem item = items[i];
            contentPanel.add(createShortcut(item.getShortcutText()));
            contentPanel.add(createDot());
            contentPanel.add(createLabel(item.getLabel()));

            if (i < items.length - 1) {
                contentPanel.add(createseparator());
            }
        }

        this.statusBar.add(contentPanel, BorderLayout.WEST);
    }

    private JLabel setStatusBarIcon() {
        JLabel label = new JLabel(icon);
        label.setBorder(border);
        return label;
    }

    private JLabel createShortcut(final String text) {
        JLabel label = new JLabel(text);
        label.setForeground(shortcutColor);
        label.setFont(font);
        return label;
    }

    private JLabel createDot() {
        JLabel label = new JLabel(innerSeparator);
        label.setForeground(dotColor);
        return label;
    }

    private JLabel createLabel(final String text) {
        JLabel label = new JLabel(text);
        label.setForeground(labelColor);
        label.setFont(font);
        return label;
    }

    private JLabel createseparator() {
        JLabel label = new JLabel(outerSeparator);
        label.setForeground(separatorColor);
        return label;
    }

    public JPanel getPanel() {
        return statusBar;
    }
}