package org.testin.editor.grid;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextArea;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.Objects;
import java.util.Optional;

public class GridCellEditor extends AbstractCellEditor implements TableCellEditor {

    private final @NotNull JBTextArea textArea = new JBTextArea();
    /**
     * The table being edited in, empty between edits.
     */
    private @NotNull Optional<JTable> editingTable = Optional.empty();
    private int editingRow = -1;

    public GridCellEditor() {
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setOpaque(true);

        final @NotNull KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        textArea.getInputMap().put(enter, "stopEditing");
        textArea.getActionMap().put("stopEditing", new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                stopCellEditing();
            }
        });

        // Ctrl+Enter inserts a line break inside the cell; plain Enter commits.
        // The break goes in at the caret, not the end of the text. Shared with
        // the expected-result field through Shortcuts.InsertNewLine, so both
        // surfaces use the one key.
        textArea.getInputMap().put(Shortcuts.InsertNewLine.getKey(), "insertNewLine");
        textArea.getActionMap().put("insertNewLine", new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                textArea.insert("\n", textArea.getCaretPosition());
                growRowToFitEditor();
            }
        });
    }

    @Override
    public @NotNull Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected, final int row, final int column) {
        editingTable = Optional.of(table);
        editingRow = row;
        textArea.setText(Objects.toString(value, ""));
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
        ApplicationManager.getApplication().invokeLater(textArea::requestFocusInWindow);
        return textArea;
    }

    /**
     * A line added with ALT+ENTER needs somewhere to go: grow the row while the
     * cell is still open, so the caret stays visible instead of typing into a
     * clipped area. The row is re-measured for real when the edit is committed.
     */
    private void growRowToFitEditor() {
        editingTable.filter(table -> editingRow >= 0 && editingRow < table.getRowCount())
                .ifPresent(this::growRowIn);
    }

    private void growRowIn(final @NotNull JTable table) {
        final int needed = textArea.getPreferredSize().height;
        if (needed > table.getRowHeight(editingRow)) {
            table.setRowHeight(editingRow, needed);
        }
    }

    @Override
    public @NotNull Object getCellEditorValue() {
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
