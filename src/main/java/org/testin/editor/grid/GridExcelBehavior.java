package org.testin.editor.grid;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.ui.table.JBTable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import org.testin.util.Shortcuts;

import java.util.Set;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Excel / DataGrip-style interaction for the grid tables:
 * <ul>
 *   <li>multi-interval cell selection (drag, Shift, Ctrl)</li>
 *   <li>clicking the order column selects the whole row;
 *       Ctrl toggles rows, Shift extends the row range</li>
 *   <li>Ctrl+C / Ctrl+X / Ctrl+V on cells using tab-separated clipboard text,
 *       compatible with Excel and DataGrip. Paste and cut go through the table
 *       model, so the normal edit listener persists the changes; read-only
 *       tables simply ignore paste/cut mutations.</li>
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GridExcelBehavior {

    public static void install(final @NotNull JBTable table) {
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setCellSelectionEnabled(true);
        table.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        installSequenceColumnRowSelection(table);
        installClipboardActions(table);
    }

    // ------------------------------------------------------------------
    // Row selection via the sequence column
    // ------------------------------------------------------------------

    private static void installSequenceColumnRowSelection(final @NotNull JBTable table) {
        // Register ahead of the UI handler: listeners run in order, and ours must
        // consume the press before the table UI applies plain cell selection.
        final MouseListener @NotNull[] existing = table.getMouseListeners();
        for (final MouseListener listener : existing) table.removeMouseListener(listener);
        table.addMouseListener(new SequenceColumnRowSelector(table));
        for (final MouseListener listener : existing) table.addMouseListener(listener);
    }

    // ------------------------------------------------------------------
    // Clipboard (TSV, Excel-compatible)
    // ------------------------------------------------------------------

    /**
     * The keys a grid answers for itself, which the editor's context menu must
     * not take back.
     * <p>
     * Every one of them means something different in a grid than on the list.
     * Copy, cut and paste are the selected cells rather than the selected test
     * case (#66, finding D1). ENTER edits the cell under the tester, or opens the
     * details on the sequence, which is {@code GridViewDetailsAction}'s job and
     * only its job.
     * <p>
     * The menu carries actions with these same keys, and
     * {@code bindShortcutsTo} registers its children on the table as IDE actions
     * - which the IDE dispatches before a component's own input map. So a menu
     * entry bound here does not merely compete, it wins. ENTER was the case that
     * proved it: the menu's ViewDetailsAction asks only whether the list has a
     * selection, the grid mirrors its selection onto the list, so it was enabled
     * on every cell and opened the details over the edit that should have
     * started.
     */
    public static @NotNull Set<KeyStroke> keysTheGridKeeps() {
        final int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        return Set.of(
                KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask),
                KeyStroke.getKeyStroke(KeyEvent.VK_X, menuMask),
                KeyStroke.getKeyStroke(KeyEvent.VK_V, menuMask),
                Shortcuts.Enter.getKey());
    }

    private static void installClipboardActions(final @NotNull JBTable table) {
        final int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        final @NotNull InputMap inputMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask), "testin.grid.copy");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuMask), "testin.grid.cut");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuMask), "testin.grid.paste");

        final @NotNull ActionMap actionMap = table.getActionMap();
        actionMap.put("testin.grid.copy", action(() -> copySelection(table, false)));
        actionMap.put("testin.grid.cut", action(() -> copySelection(table, true)));
        actionMap.put("testin.grid.paste", action(() -> pasteIntoSelection(table)));
    }

    private static @NotNull Action action(final @NotNull Runnable body) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                body.run();
            }
        };
    }

    private static void copySelection(final @NotNull JBTable table, final boolean cut) {
        final int[] rows = table.getSelectedRows();
        final int[] cols = table.getSelectedColumns();
        if (rows.length == 0 || cols.length == 0) return;

        final @NotNull StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows.length; r++) {
            if (r > 0) sb.append('\n');
            for (int c = 0; c < cols.length; c++) {
                if (c > 0) sb.append('\t');
                final @NotNull Object value = table.getValueAt(rows[r], cols[c]);
                sb.append(escapeTsvField(Objects.toString(value, "")));
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

    private static void pasteIntoSelection(final @NotNull JBTable table) {
        // An empty clipboard and a clipboard holding no text are the same
        // nothing to paste.
        final @NotNull String text = Objects.requireNonNullElse(
                CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor), "");
        if (text.isEmpty()) return;

        final int anchorRow = table.getSelectedRow();
        final int anchorCol = table.getSelectedColumn();
        if (anchorRow < 0 || anchorCol < 0) return;

        final @NotNull List<List<String>> block = parseTsv(text);
        if (block.isEmpty()) return;

        if (block.size() == 1 && block.getFirst().size() == 1) {
            // Single value: fill every selected cell, like Excel.
            final @NotNull String value = block.getFirst().getFirst();
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

            final @NotNull List<String> fields = block.get(r);
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
    private static @NotNull String escapeTsvField(final @NotNull String value) {
        if (value.indexOf('\t') < 0 && value.indexOf('\n') < 0 && value.indexOf('\r') < 0 && value.indexOf('"') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /**
     * Parses Excel-style TSV: fields separated by tabs, records by newlines,
     * quoted fields may contain tabs, newlines, and doubled quotes.
     */
    private static @NotNull List<List<String>> parseTsv(final @NotNull String text) {
        final @NotNull List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        final @NotNull StringBuilder current = new StringBuilder();
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
