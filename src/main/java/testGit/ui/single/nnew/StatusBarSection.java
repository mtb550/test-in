package testGit.ui.single.nnew;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class StatusBarSection {
    private final JPanel statusBar;

    public StatusBarSection() {
        this.statusBar = new JPanel(new BorderLayout());
        this.statusBar.setBorder(JBUI.Borders.empty(6, 10));
        this.statusBar.setOpaque(true);
        this.statusBar.setBackground(UIUtil.getPanelBackground());
        this.statusBar.add(getCreateShortcutLabel(), BorderLayout.WEST);
    }

    private @NotNull JLabel getCreateShortcutLabel() {
        String shortcutText = String.format("💡 [Enter] Save   |   [Ctrl+%s] %s   |   [Ctrl+%s] %s   |   [Ctrl+%s] %s   |   [Ctrl+%s] %s", CreateField.EXPECTED.getShortcut(), CreateField.EXPECTED.getLabel(),
                CreateField.STEPS.getShortcut(), CreateField.STEPS.getLabel(),
                CreateField.PRIORITY.getShortcut(), CreateField.PRIORITY.getLabel(),
                CreateField.GROUPS.getShortcut(), CreateField.GROUPS.getLabel());

        JLabel label = new JLabel(shortcutText);
        label.setFont(JBUI.Fonts.smallFont());
        label.setForeground(JBColor.GRAY);
        return label;
    }

    public JPanel getPanel() {
        return statusBar;
    }
}