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

package org.testin.importexport.shared;

import org.testin.editor.grid.GridPanelBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.Priority;
import org.testin.testcase.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Bundle;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.List;

public class TablePanelBuilder {

    public String @NotNull [] buildColumnNames(final @NotNull List<TestEditorAttributes> attributes) {
        final @NotNull List<String> columnNames = new ArrayList<>();
        columnNames.add("");
        columnNames.add("#");
        for (final TestEditorAttributes attr : attributes) {
            columnNames.add(attr.getName());
        }
        return columnNames.toArray(new String[0]);
    }

    // UC-SHARE-003, Rule-SHARE-017
    public @NotNull DefaultTableModel createModel(final @NotNull List<TestEditorAttributes> importAttributes, final @NotNull List<TestCaseDto> testCases) {
        final String @NotNull[] columns = buildColumnNames(importAttributes);
        final @NotNull DefaultTableModel model = new DefaultTableModel(columns, 0) {
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
            final Object @NotNull[] rowData = new Object[columns.length];
            rowData[0] = Boolean.TRUE;
            rowData[1] = String.valueOf(index++);

            for (int i = 0; i < importAttributes.size(); i++) {
                rowData[i + 2] = importAttributes.get(i).gridValue(tc);
            }
            model.addRow(rowData);
        }
        return model;
    }

    // UC-SHARE-003
    public @NotNull JBTable buildTable(final @NotNull DefaultTableModel model, final @NotNull Project p) {
        final @NotNull JBTable table = new JBTable(model);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JBTable.AUTO_RESIZE_OFF);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        final @NotNull TableColumn importColumn = table.getColumnModel().getColumn(0);

        final @NotNull JBCheckBox headerCheckbox = new JBCheckBox();
        headerCheckbox.setSelected(true);
        headerCheckbox.setHorizontalAlignment(SwingConstants.CENTER);
        headerCheckbox.setToolTipText(Bundle.message("import.select.all"));

        importColumn.setHeaderRenderer(new CheckboxHeaderRenderer(headerCheckbox));

        table.getTableHeader().addMouseListener(
                new SelectAllHeaderListener(table, model, headerCheckbox)
        );

        try {
            // By the same name buildColumnNames wrote, not by a literal that has
            // to match it. A rename of the attribute moved the column and left
            // this lookup throwing into the catch below - logged, and the editor
            // quietly without its dropdowns.
            final @NotNull TableColumn priorityCol = table.getColumn(TestEditorAttributes.PRIORITY.getName());
            final @NotNull ComboBox<String> priorityBox = new ComboBox<>();
            for (final Priority pr : Priority.values()) {
                priorityBox.addItem(pr.getLabel());
            }
            priorityCol.setCellEditor(new DefaultCellEditor(priorityBox));

            final @NotNull TableColumn groupCol = table.getColumn(TestEditorAttributes.GROUP.getName());
            groupCol.setCellEditor(new GroupMultiSelectEditor(p));
        } catch (final IllegalArgumentException ex) {
            Logger.error(ex.getMessage());
        }

        GridPanelBuilder.autoSizeColumns(table);

        return table;
    }


}
