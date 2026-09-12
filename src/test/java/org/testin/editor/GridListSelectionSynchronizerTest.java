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

package org.testin.editor;

import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.testin.editor.listeners.GridListSelectionSynchronizer;
import org.testng.annotations.Test;

import java.util.Optional;

import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;

import static org.testng.Assert.assertEquals;

public class GridListSelectionSynchronizerTest {

    @Test
    public void mapsListRowToGridRowAndPreservesColumn() {
        final JBList<String> list = new JBList<>("one", "two", "three");
        final JBTable table = new JBTable(new DefaultTableModel(3, 2));
        table.setCellSelectionEnabled(true);
        table.changeSelection(0, 1, false, false);
        list.setSelectedIndex(2);

        final GridListSelectionSynchronizer synchronizer = new GridListSelectionSynchronizer(
                list,
                () -> Optional.of(table),
                () -> true
        );
        synchronizer.valueChanged(new ListSelectionEvent(list, 2, 2, false));

        assertEquals(table.getSelectedRow(), 2);
        assertEquals(table.getSelectedColumn(), 1);
    }
}
