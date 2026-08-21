package org.testin.ui.framework;

import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * A read-only table whose rows are the tester's selection — the component for a
 * dialog that says "here is what changed, pick the ones you mean".
 * <p>
 * Read-only on purpose: a table a tester edits is the grid's job, and giving
 * this one editable cells would make the same widget mean two different things.
 * Every row starts selected, because the common answer to "which of these
 * changes?" is all of them.
 */
public final class SelectionTable implements DialogComponent {

    private final @NotNull JBTable table;
    private final @NotNull DefaultTableModel model;
    private final @NotNull JBScrollPane scroll;
    private final @NotNull JBPopupMenu rowMenu = new JBPopupMenu();

    SelectionTable(final @NotNull List<String> columns, final @NotNull List<Integer> widths) {
        model = new DefaultTableModel(columns.toArray(), 0) {
            @Override
            public boolean isCellEditable(final int row, final int column) {
                return false;
            }
        };

        table = new JBTable(model);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setFillsViewportHeight(true);

        for (int column = 0; column < widths.size() && column < columns.size(); column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(JBUI.scale(widths.get(column)));
        }

        installRowMenu();

        scroll = new JBScrollPane(table);
        scroll.setBorder(JBUI.Borders.empty(4, 12));
    }

    /**
     * Right-click selects the row under the cursor before the menu opens, so the
     * action always applies to the row the tester pointed at rather than to
     * whatever happened to be selected already.
     */
    private void installRowMenu() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(final @NotNull MouseEvent event) {
                if (!event.isPopupTrigger() && !SwingUtilities.isRightMouseButton(event)) return;
                if (rowMenu.getComponentCount() == 0) return;

                final int row = table.rowAtPoint(event.getPoint());
                if (row < 0 || row >= table.getRowCount()) {
                    table.clearSelection();
                    return;
                }

                table.setRowSelectionInterval(row, row);
                rowMenu.show(event.getComponent(), event.getX(), event.getY());
            }
        });
    }

    public void addRow(final @NotNull Object... values) {
        model.addRow(values);
    }

    /**
     * Selects every row. Called after the rows are added, because a selection
     * made before there is anything to select selects nothing.
     */
    public void selectAll() {
        if (table.getRowCount() > 0) table.addRowSelectionInterval(0, table.getRowCount() - 1);
    }

    /**
     * Selects exactly these rows, for a dialog that opens on an answer the
     * tester gave before - the groups a test case already carries. Called after
     * the rows are added, for the same reason {@link #selectAll()} is.
     */
    public void selectRows(final @NotNull List<Integer> rows) {
        table.clearSelection();
        for (final int row : rows) {
            if (row >= 0 && row < table.getRowCount()) table.addRowSelectionInterval(row, row);
        }
    }

    public @NotNull List<Integer> getSelectedRows() {
        final List<Integer> rows = new ArrayList<>();
        for (final int row : table.getSelectedRows()) rows.add(row);
        return rows;
    }

    public int getRowCount() {
        return model.getRowCount();
    }

    public @NotNull String getValueAt(final int row, final int column) {
        final Object value = model.getValueAt(row, column);
        return Objects.toString(value, "");
    }

    public void removeRow(final int row) {
        model.removeRow(row);
    }

    /**
     * Adds a right-click entry, given the row it was invoked on.
     */
    public void onRowAction(final @NotNull String label, final @NotNull IntConsumer action) {
        final JMenuItem item = new JMenuItem(label);
        item.addActionListener(event -> {
            final int row = table.getSelectedRow();
            if (row >= 0) action.accept(row);
        });
        rowMenu.add(item);
    }

    /**
     * Runs whenever the selection changes, so a dialog can follow it - a Commit
     * button has nothing to commit when no row is selected.
     */
    public void onSelectionChanged(final @NotNull Runnable listener) {
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) listener.run();
        });
    }

    @Override
    public @NotNull JComponent getPanel() {
        return scroll;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return table;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
        // Selecting a row is choosing what to include, not asking to commit -
        // the dialog's button does that, deliberately, so a stray double-click
        // on a change cannot commit the lot.
    }

    /**
     * The table is what the dialog is about, so it takes the spare height.
     */
    @Override
    public boolean fillsSpace() {
        return true;
    }
}
