package org.testin.editorPanel.grid;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.EventObject;

public class GridCellEditor extends AbstractCellEditor implements TableCellEditor {

    private final JTextArea textArea = new JTextArea();

    public GridCellEditor() {
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(BorderFactory.createEmptyBorder());
    }

    @Override
    public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected, final int row, final int column) {
        textArea.setText(value == null ? "" : value.toString());
        textArea.setFont(table.getFont());
        textArea.setBackground(table.getSelectionBackground());
        textArea.setForeground(table.getSelectionForeground());
        return textArea;
    }

    @Override
    public Object getCellEditorValue() {
        return textArea.getText();
    }

    @Override
    public boolean stopCellEditing() {
        fireEditingStopped();
        return true;
    }

    @Override
    public void cancelCellEditing() {
        fireEditingCanceled();
    }

    @Override
    public boolean isCellEditable(final EventObject e) {
        if (e instanceof MouseEvent me) {
            return me.getClickCount() >= 2;
        }
        return true;
    }
}