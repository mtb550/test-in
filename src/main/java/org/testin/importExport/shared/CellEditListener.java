package org.testin.importExport.shared;

import com.intellij.openapi.project.Project;
import org.testin.enums.TestEditorAttributes;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class CellEditListener implements TableModelListener {
    private final List<TestEditorAttributes> importAttributes;
    private final Project project;
    private final List<TestCaseDto> testCases;
    private boolean isUpdating = false;

    public CellEditListener(List<TestEditorAttributes> importAttributes, Project project, List<TestCaseDto> testCases) {
        this.importAttributes = importAttributes;
        this.project = project;
        this.testCases = testCases;
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        if (isUpdating) return;

        if (e.getType() == TableModelEvent.UPDATE) {
            int row = e.getFirstRow();
            int col = e.getColumn();

            if (row >= 0 && col >= 2) {
                isUpdating = true;
                try {
                    DefaultTableModel model = (DefaultTableModel) e.getSource();
                    String updatedValue = String.valueOf(model.getValueAt(row, col));
                    TestEditorAttributes currentAttr = importAttributes.get(col - 2);
                    TestCaseDto tc = testCases.get(row);

                    currentAttr.getImportSetter().accept(project, tc, updatedValue);

                    String formattedValue = currentAttr.getValueExtractor().apply(tc, project);
                    model.setValueAt(formattedValue, row, col);
                } finally {
                    isUpdating = false;
                }
            }
        }
    }
}
