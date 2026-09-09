package org.testin.importexport.shared;

import com.intellij.openapi.project.Project;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;

import java.util.List;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

@RequiredArgsConstructor
public class CellEditListener implements TableModelListener {
    private final @NotNull List<TestEditorAttributes> importAttributes;
    private final @NotNull Project p;
    private final @NotNull List<TestCaseDto> testCases;
    private boolean isUpdating = false;

    // UC-SHARE-003, Rule-SHARE-021
    @Override
    public void tableChanged(final @NotNull TableModelEvent e) {
        if (isUpdating) return;

        if (e.getType() == TableModelEvent.UPDATE) {
            final int row = e.getFirstRow();
            final int col = e.getColumn();

            if (row >= 0 && col >= 2) {
                isUpdating = true;
                try {
                    final @NotNull DefaultTableModel model = (DefaultTableModel) e.getSource();
                    final @NotNull String updatedValue = String.valueOf(model.getValueAt(row, col));
                    final @NotNull TestEditorAttributes currentAttr = importAttributes.get(col - 2);
                    final @NotNull TestCaseDto tc = testCases.get(row);

                    currentAttr.getImportSetter().execute(p, tc, updatedValue);

                    final @NotNull String formattedValue = currentAttr.getTestValueExtractor().execute(tc, p);
                    model.setValueAt(formattedValue, row, col);
                } finally {
                    isUpdating = false;
                }
            }
        }
    }
}
