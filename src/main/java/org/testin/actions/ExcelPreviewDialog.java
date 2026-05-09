package org.testin.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.dto.TestCaseDto;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

// todo, to be enhanced, add horizontal scroll, add remain columns, make all cells editable.
public class ExcelPreviewDialog extends DialogWrapper {
    private final List<TestCaseDto> testCases;

    public ExcelPreviewDialog(@Nullable final Project project, final List<TestCaseDto> testCases) {
        super(project, true);
        this.testCases = testCases;

        setTitle("Preview Excel Import");
        setOKButtonText("Import");
        setCancelButtonText("Cancel");

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(800, 400));

        String[] columns = {"#", "Description", "Priority", "Expected Result", "Steps Count"};

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        int index = 1;
        for (TestCaseDto tc : testCases) {
            String priority = tc.getPriority().name();
            String stepsCount = tc.getSteps().size() + " Steps";

            model.addRow(new Object[]{
                    index++,
                    tc.getDescription(),
                    priority,
                    tc.getExpectedResult(),
                    stepsCount
            });
        }

        JBTable table = new JBTable(model);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JBScrollPane scrollPane = new JBScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }
}