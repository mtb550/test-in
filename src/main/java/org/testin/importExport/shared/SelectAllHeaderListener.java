package org.testin.importExport.shared;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SelectAllHeaderListener extends MouseAdapter {
    private final DefaultTableModel model;
    private final JBCheckBox headerCheckbox;
    private final JBTable table;

    public SelectAllHeaderListener(final @NotNull JBTable table, final @NotNull DefaultTableModel model, final @NotNull JBCheckBox headerCheckbox) {
        this.table = table;
        this.model = model;
        this.headerCheckbox = headerCheckbox;
    }

    @Override
    public void mouseClicked(final @NotNull MouseEvent e) {
        int col = table.columnAtPoint(e.getPoint());
        if (col == 0) {
            boolean newState = !headerCheckbox.isSelected();
            headerCheckbox.setSelected(newState);

            for (int i = 0; i < model.getRowCount(); i++) {
                model.setValueAt(newState, i, 0);
            }
            table.getTableHeader().repaint();
        }
    }
}
