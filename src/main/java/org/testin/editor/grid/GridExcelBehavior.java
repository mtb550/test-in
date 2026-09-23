/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.editor.grid;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.ui.table.JBTable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GridExcelBehavior {
    public static void install(final @NotNull JBTable table) {
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setCellSelectionEnabled(true);
        table.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        installSequenceColumnRowSelection(table);
        installClipboardActions(table);
    }

    private static void installSequenceColumnRowSelection(final @NotNull JBTable table) {
        final MouseListener @NotNull [] existing = table.getMouseListeners();
        for (final MouseListener listener : existing) table.removeMouseListener(listener);
        table.addMouseListener(new SequenceColumnRowSelector(table));
        for (final MouseListener listener : existing) table.addMouseListener(listener);
    }

    private static void installClipboardActions(final @NotNull JBTable table) {
        final @NotNull InputMap inputMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        GridKeys.clipboard().forEach(inputMap::put);

        final @NotNull ActionMap actionMap = table.getActionMap();
        actionMap.put(GridKeys.COPY, action(() -> copySelection(table, false)));
        actionMap.put(GridKeys.CUT, action(() -> copySelection(table, true)));
        actionMap.put(GridKeys.PASTE, action(() -> pasteIntoSelection(table)));
    }

    private static @NotNull Action action(final @NotNull Runnable body) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                body.run();
            }
        };
    }

    // UC-EDITOR-PANEL-018, Rule-EDITOR-PANEL-086
    private static void copySelection(final @NotNull JBTable table, final boolean cut) {
        final int[] rows = table.getSelectedRows();
        final int[] cols = table.getSelectedColumns();
        if (rows.length == 0 || cols.length == 0) return;

        final @NotNull StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows.length; r++) {
            if (r > 0) sb.append('\n');
            for (int c = 0; c < cols.length; c++) {
                if (c > 0) sb.append('\t');
                sb.append(escapeTsvField(Objects.toString(table.getValueAt(rows[r], cols[c]), "")));
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

    // UC-EDITOR-PANEL-018, Rule-EDITOR-PANEL-088
    private static void pasteIntoSelection(final @NotNull JBTable table) {
        final @NotNull String text = Objects.requireNonNullElse(
                CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor), "");
        if (text.isEmpty()) return;

        final int anchorRow = table.getSelectedRow();
        final int anchorCol = table.getSelectedColumn();
        if (anchorRow < 0 || anchorCol < 0) return;

        final @NotNull List<List<String>> block = parseTsv(text);
        if (block.isEmpty()) return;

        if (block.size() == 1 && block.getFirst().size() == 1) {
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

    // UC-EDITOR-PANEL-018, Rule-EDITOR-PANEL-087
    private static @NotNull String escapeTsvField(final @NotNull String value) {
        if (value.indexOf('\t') < 0 && value.indexOf('\n') < 0 && value.indexOf('\r') < 0 && value.indexOf('"') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

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

        if (!current.isEmpty() || !fields.isEmpty()) {
            fields.add(current.toString());
            records.add(fields);
        }

        return records;
    }
}
