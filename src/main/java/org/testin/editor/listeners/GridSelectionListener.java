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

package org.testin.editor.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;

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

    // UC-EDITOR-PANEL-024, Rule-EDITOR-PANEL-111
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
            ApplicationManager.getApplication().invokeLater(table::requestFocusInWindow);
        }
    }
}
