package org.testin.importExport.shared;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SelectAllHeaderListener extends MouseAdapter {
    private final DefaultTableModel model;
    private final JCheckBox headerCheckbox;
    private final JTable table;

    public SelectAllHeaderListener(JTable table, DefaultTableModel model, JCheckBox headerCheckbox) {
        this.table = table;
        this.model = model;
        this.headerCheckbox = headerCheckbox;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
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
