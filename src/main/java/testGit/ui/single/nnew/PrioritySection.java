package testGit.ui.single.nnew;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Priority;

import javax.swing.*;
import java.awt.*;

public class PrioritySection {
    private final ComboBox<Priority> priority;
    private final JPanel wrapperPanel;
    Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 2f);

    public PrioritySection() {
        this.priority = new ComboBox<>(Priority.values());
        this.priority.setSelectedItem(Priority.LOW);
        this.priority.setFont(fieldFont);

        this.priority.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull final JList<? extends Priority> list, final Priority value, final int index, final boolean selected, final boolean hasFocus) {
                if (value != null) {
                    setIcon(value.getIcon());
                    append(" Priority: " + value.name());
                }
            }
        });

        this.wrapperPanel = new JPanel(new BorderLayout());
        this.wrapperPanel.setOpaque(false);
        JLabel iconLabel = new JLabel(CreateField.PRIORITY.getIcon());
        iconLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 8));
        this.wrapperPanel.add(iconLabel, BorderLayout.WEST);
        this.wrapperPanel.add(this.priority, BorderLayout.CENTER);
        this.wrapperPanel.setBorder(JBUI.Borders.emptyTop(8));
    }

    public void showSection(JPanel contentPanel) {
        if (wrapperPanel.getParent() == null)
            contentPanel.add(wrapperPanel);
        priority.requestFocus();
    }

    public ComboBox<Priority> getCombo() {
        return priority;
    }

    public JPanel getWrapper() {
        return wrapperPanel;
    }
}