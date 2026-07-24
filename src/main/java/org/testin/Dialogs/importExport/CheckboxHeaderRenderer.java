package org.testin.Dialogs.importExport;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class CheckboxHeaderRenderer implements TableCellRenderer {
    private final JCheckBox headerCheckbox;

    public CheckboxHeaderRenderer(JCheckBox headerCheckbox) {
        this.headerCheckbox = headerCheckbox;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JTableHeader header = table.getTableHeader();
        headerCheckbox.setBackground(header.getBackground());
        headerCheckbox.setForeground(header.getForeground());
        headerCheckbox.setFont(header.getFont());
        headerCheckbox.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        return headerCheckbox;
    }
}
