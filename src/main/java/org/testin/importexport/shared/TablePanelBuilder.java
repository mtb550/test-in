package org.testin.importexport.shared;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import java.util.Optional;
import org.testin.logger.Logger;
import org.testin.model.Priority;
import org.testin.model.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TablePanelBuilder {

    public String @NotNull [] buildColumnNames(final @NotNull List<TestEditorAttributes> attributes) {
        final List<String> columnNames = new ArrayList<>();
        columnNames.add("");
        columnNames.add("#");
        for (final TestEditorAttributes attr : attributes) {
            columnNames.add(attr.getName());
        }
        return columnNames.toArray(new String[0]);
    }

    public @NotNull DefaultTableModel createModel(final @NotNull Project p,
                                                  final @NotNull List<TestEditorAttributes> importAttributes,
                                                  final @NotNull List<TestCaseDto> testCases) {
        final String[] columns = buildColumnNames(importAttributes);
        final DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public @NotNull Class<?> getColumnClass(final int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(final int row, final int column) {
                return column == 0 || column >= 2;
            }
        };

        int index = 1;
        for (final TestCaseDto tc : testCases) {
            final Object[] rowData = new Object[columns.length];
            rowData[0] = Boolean.TRUE;
            rowData[1] = String.valueOf(index++);

            for (int i = 0; i < importAttributes.size(); i++) {
                rowData[i + 2] = importAttributes.get(i).getTestValueExtractor().execute(tc, p);
            }
            model.addRow(rowData);
        }
        return model;
    }

    public @NotNull JBTable buildTable(final @NotNull DefaultTableModel model, final @NotNull Project p) {
        final JBTable table = new JBTable(model);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JBTable.AUTO_RESIZE_OFF);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        final TableColumn importColumn = table.getColumnModel().getColumn(0);

        final JBCheckBox headerCheckbox = new JBCheckBox();
        headerCheckbox.setSelected(true);
        headerCheckbox.setHorizontalAlignment(SwingConstants.CENTER);
        headerCheckbox.setToolTipText("Select All / Deselect All");

        importColumn.setHeaderRenderer(new CheckboxHeaderRenderer(headerCheckbox));

        table.getTableHeader().addMouseListener(
                new SelectAllHeaderListener(table, model, headerCheckbox)
        );

        try {
            final TableColumn priorityCol = table.getColumn("Priority");
            final ComboBox<String> priorityBox = new ComboBox<>();
            for (final Priority pr : Priority.values()) {
                priorityBox.addItem(pr.getName());
            }
            priorityCol.setCellEditor(new DefaultCellEditor(priorityBox));

            final TableColumn groupCol = table.getColumn("Group");
            groupCol.setCellEditor(new GroupMultiSelectEditor(p));
        } catch (final IllegalArgumentException ex) {
            Logger.error(ex.getMessage());
        }

        autoSizeColumns(table);

        return table;
    }

    public void autoSizeColumns(final @NotNull JBTable table) {
        int tableTotalWidth = 0;
        for (int i = 0; i < table.getColumnCount(); i++) {
            final TableColumn col = table.getColumnModel().getColumn(i);

            final TableCellRenderer headerRenderer = Optional.ofNullable(col.getHeaderRenderer())
                    .orElseGet(() -> table.getTableHeader().getDefaultRenderer());
            final Component headerComp = headerRenderer.getTableCellRendererComponent(
                    table, col.getHeaderValue(), false, false, 0, i);
            int maxWidth = headerComp.getPreferredSize().width;

            for (int r = 0; r < table.getRowCount(); r++) {
                final TableCellRenderer renderer = table.getCellRenderer(r, i);
                final Component comp = table.prepareRenderer(renderer, r, i);
                maxWidth = Math.max(comp.getPreferredSize().width, maxWidth);
            }

            maxWidth += 20;
            col.setPreferredWidth(maxWidth);
            tableTotalWidth += maxWidth;
        }

        final int tableTotalHeight = table.getRowHeight() * Math.max(3, table.getRowCount());
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        table.setPreferredScrollableViewportSize(new Dimension(
                Math.min(tableTotalWidth, (int) (screenSize.width * 0.85)),
                Math.min(tableTotalHeight, (int) (screenSize.height * 0.70))
        ));
    }

}
