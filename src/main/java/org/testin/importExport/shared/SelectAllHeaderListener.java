package org.testin.importExport.shared;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.table.JBTable;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@AllArgsConstructor
public class SelectAllHeaderListener extends MouseAdapter {
    private final @NotNull JBTable table;
    private final @NotNull DefaultTableModel model;
    private final @NotNull JBCheckBox headerCheckbox;

    @Override
    public void mouseClicked(final @NotNull MouseEvent e) {
        final int col = table.columnAtPoint(e.getPoint());
        if (col == 0) {
            final boolean newState = !headerCheckbox.isSelected();
            headerCheckbox.setSelected(newState);

            for (int i = 0; i < model.getRowCount(); i++) {
                model.setValueAt(newState, i, 0);
            }
            table.getTableHeader().repaint();
        }
    }
}
