package testGit.ui.single.nnew;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Priority;
import testGit.ui.bulk.UpdateField;

import javax.swing.*;
import java.awt.*;

public class PrioritySection {
    private final ComboBox<Priority> priorityCombo;
    private final JPanel wrapperPanel;
    private final float fontSize = JBUI.Fonts.label().getSize2D() + 2f;

    public PrioritySection() {
        // 1. بناء القائمة المنسدلة (ComboBox)
        this.priorityCombo = new ComboBox<>(Priority.values());
        this.priorityCombo.setSelectedItem(Priority.LOW); // القيمة الافتراضية
        this.priorityCombo.setFont(JBFont.regular().deriveFont(fontSize));
        this.priorityCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends Priority> list, Priority value, int index, boolean selected, boolean hasFocus) {
                if (value != null) {
                    setIcon(value.getIcon());
                    append(" Priority: " + value.name());
                }
            }
        });

        // 2. تغليف الحقل في JPanel مع إضافة الأيقونة الجانبية
        this.wrapperPanel = new JPanel(new BorderLayout());
        this.wrapperPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(UpdateField.PRIORITY.getIcon());
        iconLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 8));

        this.wrapperPanel.add(iconLabel, BorderLayout.WEST);
        this.wrapperPanel.add(this.priorityCombo, BorderLayout.CENTER);
        this.wrapperPanel.setBorder(JBUI.Borders.emptyTop(8));
    }

    public Runnable getShowAction(JPanel contentPanel, Runnable repackPopup) {
        return () -> {
            if (wrapperPanel.getParent() == null) {
                contentPanel.add(wrapperPanel);
            }
            repackPopup.run();
            priorityCombo.requestFocus();
        };
    }

    public ComboBox<Priority> getCombo() {
        return priorityCombo;
    }

    public JPanel getWrapper() {
        return wrapperPanel;
    }
}