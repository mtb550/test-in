package org.testin.editor.listeners;

import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.Arrays;
import java.util.List;

/**
 * Keeps the list's selection on whatever the grid shows selected.
 * <p>
 * The list is the one place that answers "which test cases has the tester
 * chosen" - every action in both editors asks it, none of them knows which view
 * is on screen, and the grid is not in the component tree the list lives in. So
 * the bridge is what makes a grid selection real, and it carries the whole
 * selection rather than the lead row: eight rows selected and F pressed fails
 * eight cases, the way it does in list view (#74).
 */
@AllArgsConstructor
public class GridSelectionListener implements ListSelectionListener {

    private final @NotNull TestinEditor editor;
    private final @NotNull JBTable table;
    private final @NotNull JBList<TestCaseDto> list;
    private final @NotNull List<TestCaseDto> pageItems;

    @Override
    public void valueChanged(final ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;

        final int @NotNull [] rows = Arrays.stream(table.getSelectedRows())
                .filter(row -> row >= 0 && row < pageItems.size())
                .toArray();

        if (rows.length == 0) return;

        final boolean gridHadFocus = table.isFocusOwner();

        // The lead row first, because it is what the details panel follows and
        // what a page change would be about.
        editor.selectTestCase(pageItems.get(rows[0]));
        // Then the rest, over the single selection that call just made.
        list.setSelectedIndices(rows);

        if (gridHadFocus) {
            // Selecting synchronizes the list and may move focus to it.
            SwingUtilities.invokeLater(table::requestFocusInWindow);
        }
    }
}
