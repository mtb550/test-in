package org.testin.editorPanel.listeners;

import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Keeps the visible grid cell aligned with the list selection.
 */
public final class GridListSelectionSynchronizer implements ListSelectionListener {
    private final @NotNull JBList<?> list;
    private final @NotNull Supplier<JBTable> tableSupplier;
    private final @NotNull BooleanSupplier gridActiveSupplier;

    public GridListSelectionSynchronizer(final @NotNull JBList<?> list,
                                         final @NotNull Supplier<JBTable> tableSupplier,
                                         final @NotNull BooleanSupplier gridActiveSupplier) {
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
