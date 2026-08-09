package org.testin.editorPanel.listeners;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestEditorAttributes;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class GridEditListener implements TableModelListener {
    private final @NotNull Project p;
    private final List<TestCaseDto> pageItems;
    private final Runnable onEdited;
    private boolean updating = false;

    public GridEditListener(final @NotNull Project p, final List<TestCaseDto> pageItems, final Runnable onEdited) {
        this.p = p;
        this.pageItems = pageItems;
        this.onEdited = onEdited;
    }

    @Override
    public void tableChanged(final TableModelEvent e) {
        if (updating) return;
        if (e.getType() != TableModelEvent.UPDATE) return;
        final int row = e.getFirstRow();
        final int col = e.getColumn();
        if (row < 0 || col <= 0) return;
        updating = true;
        try {
            final DefaultTableModel model = (DefaultTableModel) e.getSource();
            final TestEditorAttributes attr = TestEditorAttributes.values()[col - 1];
            final TestCaseDto tc = pageItems.get(row);
            attr.getImportSetter().execute(p, tc, String.valueOf(model.getValueAt(row, col)));
            model.setValueAt(attr.getTestValueExtractor().execute(tc, p), row, col);
            onEdited.run();
        } finally {
            updating = false;
        }
    }
}
