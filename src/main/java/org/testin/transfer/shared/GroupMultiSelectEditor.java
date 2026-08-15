package org.testin.transfer.shared;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

public class GroupMultiSelectEditor extends AbstractCellEditor implements TableCellEditor {
    private final @NotNull JButton button = new JButton();
    private @NotNull String currentValue = "";

    public GroupMultiSelectEditor(final @NotNull Project p) {
        button.setBorderPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBackground(UIManager.getColor("Table.selectionBackground"));
        button.setForeground(UIManager.getColor("Table.selectionForeground"));

        button.addActionListener(e -> {
            final GroupSelectionDialog dialog = new GroupSelectionDialog(p, currentValue);
            if (dialog.showAndGet()) {
                currentValue = dialog.getSelectedGroupsStr();
            }
            fireEditingStopped();
        });
    }

    @Override
    public @NotNull Component getTableCellEditorComponent(final @NotNull JTable table, final @Nullable Object value,
                                                          final boolean isSelected, final int row, final int column) {
        currentValue = value != null ? value.toString() : "";
        button.setText(currentValue);
        return button;
    }

    @Override
    public @NotNull Object getCellEditorValue() {
        return currentValue;
    }
}
