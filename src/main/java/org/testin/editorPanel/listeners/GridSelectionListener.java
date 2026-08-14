package org.testin.editorPanel.listeners;

import com.intellij.ui.table.JBTable;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditor;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.List;

@AllArgsConstructor
public class GridSelectionListener implements ListSelectionListener {
    private final @NotNull IEditor editor;
    private final @NotNull JBTable table;
    private final @NotNull List<TestCaseDto> pageItems;

    @Override
    public void valueChanged(final ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        final int row = table.getSelectedRow();
        if (row >= 0 && row < pageItems.size()) {
            final boolean gridHadFocus = table.isFocusOwner();
            editor.selectTestCase(pageItems.get(row));
            if (gridHadFocus) {
                // selectTestCase synchronizes the list and may move focus to it.
                SwingUtilities.invokeLater(table::requestFocusInWindow);
            }
        }
    }
}
