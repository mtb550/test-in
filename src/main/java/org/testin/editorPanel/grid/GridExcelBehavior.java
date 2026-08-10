package org.testin.editorPanel.grid;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel / DataGrip-style interaction for the grid tables:
 * <ul>
 *   <li>multi-interval cell selection (drag, Shift, Ctrl)</li>
 *   <li>clicking the sequence ("#") column selects the whole row;
 *       Ctrl toggles rows, Shift extends the row range</li>
 *   <li>Ctrl+C / Ctrl+X / Ctrl+V on cells using tab-separated clipboard text,
 *       compatible with Excel and DataGrip. Paste and cut go through the table
 *       model, so the normal edit listener persists the changes; read-only
 *       tables simply ignore paste/cut mutations.</li>
 * </ul>
 */
public final class GridExcelBehavior {

    private GridExcelBehavior() {
    }

    public static void install(final JBTable table) {
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setCellSelectionEnabled(true);
        table.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        installSequenceColumnRowSelection(table);
        installClipboardActions(table);
    }

    // ------------------------------------------------------------------
    // Row selection via the sequence column
    // ------------------------------------------------------------------

    private static void installSequenceColumnRowSelection(final JBTable table) {
        final MouseAdapter rowSelector = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || e.isPopupTrigger()) return;

                final int viewRow = table.rowAtPoint(e.getPoint());
                final int viewCol = table.columnAtPoint(e.getPoint());
                if (viewRow < 0 || viewCol < 0) return;
                if (table.convertColumnIndexToModel(viewCol) != 0) return; // only the "#" column

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
        };

        // Register ahead of the UI handler: listeners run in order, and ours must
        // consume the press before the table UI applies plain cell selection.
        final MouseListener[] existing = table.getMouseListeners();
        for (final MouseListener listener : existing) table.removeMouseListener(listener);
        table.addMouseListener(rowSelector);
        for (final MouseListener listener : existing) table.addMouseListener(listener);
    }

    // ------------------------------------------------------------------
    // Clipboard (TSV, Excel-compatible)
    // ------------------------------------------------------------------

    private static void installClipboardActions(final JBTable table) {
        final int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        final InputMap inputMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask), "testin.grid.copy");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuMask), "testin.grid.cut");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuMask), "testin.grid.paste");

        final ActionMap actionMap = table.getActionMap();
        actionMap.put("testin.grid.copy", action(() -> copySelection(table, false)));
        actionMap.put("testin.grid.cut", action(() -> copySelection(table, true)));
        actionMap.put("testin.grid.paste", action(() -> pasteIntoSelection(table)));
    }

    private static Action action(final Runnable body) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                body.run();
            }
        };
    }

    private static void copySelection(final JBTable table, final boolean cut) {
        final int[] rows = table.getSelectedRows();
        final int[] cols = table.getSelectedColumns();
        if (rows.length == 0 || cols.length == 0) return;

        final StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows.length; r++) {
            if (r > 0) sb.append('\n');
            for (int c = 0; c < cols.length; c++) {
                if (c > 0) sb.append('\t');
                final Object value = table.getValueAt(rows[r], cols[c]);
                sb.append(escapeTsvField(value == null ? "" : value.toString()));
            }
        }
        CopyPasteManager.getInstance().setContents(new StringSelection(sb.toString()));

        if (cut) {
            for (final int row : rows) {
                for (final int col : cols) {
                    if (table.isCellEditable(row, col)) {
                        table.setValueAt("", row, col);
                    }
                }
            }
        }
    }

    private static void pasteIntoSelection(final JBTable table) {
        final String text = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
        if (text == null || text.isEmpty()) return;

        final int anchorRow = table.getSelectedRow();
        final int anchorCol = table.getSelectedColumn();
        if (anchorRow < 0 || anchorCol < 0) return;

        final List<List<String>> block = parseTsv(text);
        if (block.isEmpty()) return;

        if (block.size() == 1 && block.getFirst().size() == 1) {
            // Single value: fill every selected cell, like Excel.
            final String value = block.getFirst().getFirst();
            for (final int row : table.getSelectedRows()) {
                for (final int col : table.getSelectedColumns()) {
                    if (table.isCellEditable(row, col)) {
                        table.setValueAt(value, row, col);
                    }
                }
            }
            return;
        }

        // Block paste anchored at the top-left selected cell.
        for (int r = 0; r < block.size(); r++) {
            final int row = anchorRow + r;
            if (row >= table.getRowCount()) break;

            final List<String> fields = block.get(r);
            for (int c = 0; c < fields.size(); c++) {
                final int col = anchorCol + c;
                if (col >= table.getColumnCount()) break;
                if (table.isCellEditable(row, col)) {
                    table.setValueAt(fields.get(c), row, col);
                }
            }
        }
    }

    /**
     * Excel TSV quoting: fields containing tabs, newlines, or quotes are wrapped
     * in double quotes with internal quotes doubled.
     */
    private static String escapeTsvField(final String value) {
        if (value.indexOf('\t') < 0 && value.indexOf('\n') < 0 && value.indexOf('\r') < 0 && value.indexOf('"') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /**
     * Parses Excel-style TSV: fields separated by tabs, records by newlines,
     * quoted fields may contain tabs, newlines, and doubled quotes.
     */
    private static List<List<String>> parseTsv(final String text) {
        final List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"' && current.isEmpty()) {
                inQuotes = true;
            } else if (c == '\t') {
                fields.add(current.toString());
                current.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                fields.add(current.toString());
                current.setLength(0);
                records.add(fields);
                fields = new ArrayList<>();
            } else {
                current.append(c);
            }
        }

        // Trailing record without a final newline.
        if (!current.isEmpty() || !fields.isEmpty()) {
            fields.add(current.toString());
            records.add(fields);
        }

        return records;
    }
}
