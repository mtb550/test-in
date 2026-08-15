package org.testin.editor;

import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.testin.editor.listeners.GridListSelectionSynchronizer;
import org.testng.annotations.Test;

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
                () -> table,
                () -> true
        );
        synchronizer.valueChanged(new ListSelectionEvent(list, 2, 2, false));

        assertEquals(table.getSelectedRow(), 2);
        assertEquals(table.getSelectedColumn(), 1);
    }
}
