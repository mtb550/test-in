package org.testin.editorPanel.grid;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextArea;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

public class GridCellEditor extends AbstractCellEditor implements TableCellEditor {

    private final JBTextArea textArea = new JBTextArea();

    public GridCellEditor() {
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setOpaque(true);

        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        textArea.getInputMap().put(enter, "stopEditing");
        textArea.getActionMap().put("stopEditing", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopCellEditing();
            }
        });

        KeyStroke shiftEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK);
        textArea.getInputMap().put(shiftEnter, "insertNewLine");
        textArea.getActionMap().put("insertNewLine", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.append("\n");
            }
        });
    }

    @Override
    public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected, final int row, final int column) {
        textArea.setText(value == null ? "" : value.toString());
        textArea.setFont(table.getFont());
        textArea.setBackground(table.getSelectionBackground());
        textArea.setForeground(table.getForeground());
        textArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.blue, 1),
                BorderFactory.createEmptyBorder(GridPanelBuilder.CELL_PADDING, GridPanelBuilder.CELL_PADDING, GridPanelBuilder.CELL_PADDING, GridPanelBuilder.CELL_PADDING)));

        // Returned directly: JTable sizes the editor component to the full cell
        // rectangle, so the border outlines the cell. The old wrapper panel gave the
        // text area only its preferred height, leaving the border hugging a single
        // text line in the middle of tall (word-wrapped) rows.
        SwingUtilities.invokeLater(textArea::requestFocusInWindow);
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
