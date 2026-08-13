package org.testin.importExport.shared;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestEditorAttributes;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class CellEditListener implements TableModelListener {
    private final @NotNull List<TestEditorAttributes> importAttributes;
    private final @NotNull Project p;
    private final @NotNull List<TestCaseDto> testCases;
    private boolean isUpdating = false;

    public CellEditListener(final @NotNull List<TestEditorAttributes> importAttributes, final @NotNull Project p,
                            final @NotNull List<TestCaseDto> testCases) {
        this.importAttributes = importAttributes;
        this.p = p;
        this.testCases = testCases;
    }

    @Override
    public void tableChanged(final @NotNull TableModelEvent e) {
        if (isUpdating) return;

        if (e.getType() == TableModelEvent.UPDATE) {
            final int row = e.getFirstRow();
            final int col = e.getColumn();

            if (row >= 0 && col >= 2) {
                isUpdating = true;
                try {
                    final DefaultTableModel model = (DefaultTableModel) e.getSource();
                    final String updatedValue = String.valueOf(model.getValueAt(row, col));
                    final TestEditorAttributes currentAttr = importAttributes.get(col - 2);
                    final TestCaseDto tc = testCases.get(row);

                    currentAttr.getImportSetter().execute(p, tc, updatedValue);

                    final String formattedValue = currentAttr.getTestValueExtractor().execute(tc, p);
                    model.setValueAt(formattedValue, row, col);
                } finally {
                    isUpdating = false;
                }
            }
        }
    }
}
