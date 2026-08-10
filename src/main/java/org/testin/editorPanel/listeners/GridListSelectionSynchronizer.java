package org.testin.editorPanel.listeners;

import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Keeps the visible grid cell aligned with the list selection.
 */
public final class GridListSelectionSynchronizer implements ListSelectionListener {
    private final JBList<?> list;
    private final Supplier<JBTable> tableSupplier;
    private final BooleanSupplier gridActiveSupplier;

    public GridListSelectionSynchronizer(final JBList<?> list,
                                         final Supplier<JBTable> tableSupplier,
                                         final BooleanSupplier gridActiveSupplier) {
        this.list = list;
        this.tableSupplier = tableSupplier;
        this.gridActiveSupplier = gridActiveSupplier;
    }

    @Override
    public void valueChanged(final ListSelectionEvent event) {
        if (event.getValueIsAdjusting() || !gridActiveSupplier.getAsBoolean()) return;

        final JBTable table = tableSupplier.get();
        if (table == null) return;

        final int row = list.getSelectedIndex();
        if (row < 0 || row >= table.getRowCount() || row == table.getSelectedRow()) return;

        table.changeSelection(row, Math.max(0, table.getSelectedColumn()), false, false);
    }
}
