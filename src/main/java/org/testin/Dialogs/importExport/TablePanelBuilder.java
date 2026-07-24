package org.testin.Dialogs.importExport;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import org.testin.pojo.Priority;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.logger.Log;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TablePanelBuilder {

    public String[] buildColumnNames(List<TestEditorAttributes> attributes) {
        List<String> columnNames = new ArrayList<>();
        columnNames.add("");
        columnNames.add("#");
        for (TestEditorAttributes attr : attributes) {
            columnNames.add(attr.getName());
        }
        return columnNames.toArray(new String[0]);
    }

    public DefaultTableModel createModel(Project project, List<TestEditorAttributes> importAttributes, List<TestCaseDto> testCases) {
        String[] columns = buildColumnNames(importAttributes);
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column >= 2;
            }
        };

        int index = 1;
        for (TestCaseDto tc : testCases) {
            Object[] rowData = new Object[columns.length];
            rowData[0] = Boolean.TRUE;
            rowData[1] = String.valueOf(index++);

            for (int i = 0; i < importAttributes.size(); i++) {
                rowData[i + 2] = importAttributes.get(i).getValueExtractor().apply(tc, project);
            }
            model.addRow(rowData);
        }
        return model;
    }

    public JBTable buildTable(DefaultTableModel model, Project project) {
        JBTable table = new JBTable(model);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        TableColumn importColumn = table.getColumnModel().getColumn(0);

        JCheckBox headerCheckbox = new JCheckBox();
        headerCheckbox.setSelected(true);
        headerCheckbox.setHorizontalAlignment(SwingConstants.CENTER);
        headerCheckbox.setToolTipText("Select All / Deselect All");

        importColumn.setHeaderRenderer(new CheckboxHeaderRenderer(headerCheckbox));

        table.getTableHeader().addMouseListener(
                new SelectAllHeaderListener(table, model, headerCheckbox)
        );

        try {
            TableColumn priorityCol = table.getColumn("Priority");
            ComboBox<String> priorityBox = new ComboBox<>();
            for (Priority p : Priority.values()) {
                priorityBox.addItem(p.getName());
            }
            priorityCol.setCellEditor(new DefaultCellEditor(priorityBox));

            TableColumn groupCol = table.getColumn("Group");
            groupCol.setCellEditor(new GroupMultiSelectEditor(project));
        } catch (final IllegalArgumentException ex) {
            Log.error(ex.getMessage());
        }

        autoSizeColumns(table);

        return table;
    }

    public void autoSizeColumns(JBTable table) {
        int tableTotalWidth = 0;
        for (int i = 0; i < table.getColumnCount(); i++) {
            TableColumn col = table.getColumnModel().getColumn(i);
            int maxWidth;

            TableCellRenderer headerRenderer = col.getHeaderRenderer();
            if (headerRenderer == null) {
                headerRenderer = table.getTableHeader().getDefaultRenderer();
            }
            Component headerComp = headerRenderer.getTableCellRendererComponent(
                    table, col.getHeaderValue(), false, false, 0, i);
            maxWidth = headerComp.getPreferredSize().width;

            for (int r = 0; r < table.getRowCount(); r++) {
                TableCellRenderer renderer = table.getCellRenderer(r, i);
                Component comp = table.prepareRenderer(renderer, r, i);
                maxWidth = Math.max(comp.getPreferredSize().width, maxWidth);
            }

            maxWidth += 20;
            col.setPreferredWidth(maxWidth);
            tableTotalWidth += maxWidth;
        }

        int tableTotalHeight = table.getRowHeight() * Math.max(3, table.getRowCount());
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        table.setPreferredScrollableViewportSize(new Dimension(
                Math.min(tableTotalWidth, (int) (screenSize.width * 0.85)),
                Math.min(tableTotalHeight, (int) (screenSize.height * 0.70))
        ));
    }

    public JBTabbedPane createTabbedPane(Map<String, List<TestCaseDto>> sheetsData, List<TestEditorAttributes> attributes, Project project, Consumer<DefaultTableModel> modelCustomizer) {
        JBTabbedPane tabbedPane = new JBTabbedPane();

        for (Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
            String sheetName = entry.getKey();
            List<TestCaseDto> testCases = entry.getValue();

            DefaultTableModel model = createModel(project, attributes, testCases);
            modelCustomizer.accept(model);

            JBTable table = buildTable(model, project);
            tabbedPane.addTab(sheetName, new JBScrollPane(table));
        }

        return tabbedPane;
    }
}
