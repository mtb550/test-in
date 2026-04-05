package testGit.ui.single.nnew;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

public class StatusBarSection {
    private final JPanel statusBar;
    private final Color shortcutColor = JBColor.GRAY;
    private final Color dotColor = JBColor.GRAY;
    private final Color labelColor = JBColor.GRAY;
    private final Color separatorColor = JBColor.GRAY;
    private final Font font = JBUI.Fonts.smallFont();

    public StatusBarSection() {
        this.statusBar = new JPanel(new BorderLayout());
        this.statusBar.setBorder(JBUI.Borders.empty(6, 10));
        this.statusBar.setOpaque(true);
        this.statusBar.setBackground(UIUtil.getPanelBackground());

        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        contentPanel.setOpaque(false);

        contentPanel.add(createLabel("💡 "));
        contentPanel.add(createShortcut("Enter"));
        contentPanel.add(createDot());
        contentPanel.add(createLabel("Save"));
        contentPanel.add(createseparator());

        for (CreateField field : CreateField.values()) {
            String shortcutText = field.getShortcut().getShortcutText();

            contentPanel.add(createShortcut(shortcutText));
            contentPanel.add(createDot());
            contentPanel.add(createLabel(field.getLabel()));
            contentPanel.add(createseparator());
        }

        this.statusBar.add(contentPanel, BorderLayout.WEST);
    }

    private JLabel createShortcut(final String text) {
        JLabel label = new JLabel(text);
        label.setForeground(shortcutColor);
        label.setFont(font);
        return label;
    }

    private JLabel createDot() {
        JLabel label = new JLabel(":");
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
        JLabel label = new JLabel("   ");
        label.setForeground(separatorColor);
        return label;
    }

    public JPanel getPanel() {
        return statusBar;
    }
}