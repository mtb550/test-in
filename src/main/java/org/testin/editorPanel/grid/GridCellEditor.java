package org.testin.editorPanel.grid;

import com.intellij.ui.components.JBTextField;

import javax.swing.*;
import java.awt.*;

public class GridCellEditor extends DefaultCellEditor {

    public GridCellEditor() {
        super(new JBTextField());
    }

    @Override
    public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected, final int row, final int column) {
        final Component editor = super.getTableCellEditorComponent(table, value, isSelected, row, column);
        editor.setFont(table.getFont());
        return editor;
    }
}
