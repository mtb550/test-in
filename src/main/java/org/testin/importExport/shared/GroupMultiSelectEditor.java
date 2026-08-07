package org.testin.importExport.shared;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

public class GroupMultiSelectEditor extends AbstractCellEditor implements TableCellEditor {
    private final JButton button = new JButton();
    private String currentValue = "";

    public GroupMultiSelectEditor(final @NotNull Project p) {
        button.setBorderPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBackground(UIManager.getColor("Table.selectionBackground"));
        button.setForeground(UIManager.getColor("Table.selectionForeground"));

        button.addActionListener(e -> {
            GroupSelectionDialog dialog = new GroupSelectionDialog(p, currentValue);
            if (dialog.showAndGet()) {
                currentValue = dialog.getSelectedGroupsStr();
            }
            fireEditingStopped();
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        currentValue = value != null ? value.toString() : "";
        button.setText(currentValue);
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        return currentValue;
    }
}
