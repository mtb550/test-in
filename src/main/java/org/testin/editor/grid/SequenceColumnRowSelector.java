package org.testin.editor.grid;

import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Excel-style row selection via the order column: plain click selects
 * the whole row, Ctrl toggles rows, Shift extends the row range. Must be
 * registered ahead of the table UI's mouse handler and consumes the press so
 * default cell selection cannot override the row selection.
 */
final class SequenceColumnRowSelector extends MouseAdapter {

    private final @NotNull JBTable table;

    SequenceColumnRowSelector(final @NotNull JBTable table) {
        this.table = table;
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e) || e.isPopupTrigger()) return;

        final int viewRow = table.rowAtPoint(e.getPoint());
        final int viewCol = table.columnAtPoint(e.getPoint());
        if (viewRow < 0 || viewCol < 0) return;
        if (!GridPanelBuilder.isOrderColumn(table, viewCol)) return;

        final ListSelectionModel rows = table.getSelectionModel();

        if (e.isShiftDown()) {
            final int anchor = rows.getAnchorSelectionIndex();
            rows.setSelectionInterval(anchor < 0 ? viewRow : anchor, viewRow);
        } else if (e.isControlDown() || e.isMetaDown()) {
            if (rows.isSelectedIndex(viewRow)) {
                rows.removeSelectionInterval(viewRow, viewRow);
            } else {
                rows.addSelectionInterval(viewRow, viewRow);
            }
        } else {
            rows.setSelectionInterval(viewRow, viewRow);
        }

        table.setColumnSelectionInterval(0, table.getColumnCount() - 1);
        table.requestFocusInWindow();

        // Consumed events are ignored by BasicTableUI, so the default
        // cell-selection handling cannot override the row selection.
        e.consume();
    }
}
