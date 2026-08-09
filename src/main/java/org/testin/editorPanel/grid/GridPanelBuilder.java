package org.testin.editorPanel.grid;

import com.intellij.openapi.project.Project;
import com.intellij.ui.table.JBTable;
import org.testin.enums.RunEditorAttributes;
import org.testin.enums.TestEditorAttributes;
import org.testin.logger.Logger;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class GridPanelBuilder {

    public static void resizeToFont(final JBTable table) {
        final FontMetrics fm = table.getFontMetrics(table.getFont());
        table.setRowHeight(Math.max(fm.getHeight() + 4, 20));
        autoSizeColumns(table);
    }

    private static void autoSizeColumns(final JBTable table) {
        int tableTotalWidth = 0;
        for (int i = 0; i < table.getColumnCount(); i++) {
            final TableColumn col = table.getColumnModel().getColumn(i);
            int maxWidth;

            TableCellRenderer headerRenderer = col.getHeaderRenderer();
            if (headerRenderer == null) {
                headerRenderer = table.getTableHeader().getDefaultRenderer();
            }
            final Component headerComp = headerRenderer.getTableCellRendererComponent(
                    table, col.getHeaderValue(), false, false, 0, i);
            maxWidth = headerComp.getPreferredSize().width;

            for (int r = 0; r < table.getRowCount(); r++) {
                final TableCellRenderer renderer = table.getCellRenderer(r, i);
                final Component comp = table.prepareRenderer(renderer, r, i);
                maxWidth = Math.max(comp.getPreferredSize().width, maxWidth);
            }

            maxWidth += 20;
            col.setPreferredWidth(maxWidth);
            tableTotalWidth += maxWidth;
        }

        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        table.setPreferredScrollableViewportSize(new Dimension(
                Math.min(tableTotalWidth, (int) (screenSize.width * 0.85)),
                Math.min(table.getRowHeight() * Math.max(3, table.getRowCount()), (int) (screenSize.height * 0.70))
        ));
    }

    public JBTable buildRunTable(final Project p, final List<TestCaseDto> testCases, final Set<RunEditorAttributes> attributes, final Map<UUID, TestRunItems> resultsMap) {
        Logger.debug("[GridPanelBuilder] buildRunTable: testCases=" + testCases.size() + ", attributes=" + attributes);
        final List<RunEditorAttributes> ordered = Arrays.stream(RunEditorAttributes.values()).toList();

        final String[] columns = buildColumns(ordered, RunEditorAttributes::getName);
        final List<String[]> rows = new ArrayList<>();

        int index = 1;
        for (final TestCaseDto tc : testCases) {
            final TestRunItems runItem = resultsMap.get(tc.getId());
            if (runItem == null) continue;

            final String[] row = new String[columns.length];
            row[0] = String.valueOf(index++);
            int c = 1;
            for (final RunEditorAttributes attr : ordered) {
                row[c++] = attr.getRunValueExtractor().execute(runItem, p);
            }
            rows.add(row);
        }

        final JBTable table = buildTable(columns, rows, false);
        applyColumnVisibility(table, ordered, RunEditorAttributes::getName, attributes);
        return table;
    }

    public JBTable buildTestTable(final Project p, final List<TestCaseDto> testCases, final Set<TestEditorAttributes> attributes) {
        Logger.debug("[GridPanelBuilder] buildTestTable: testCases=" + testCases.size() + ", attributes=" + attributes);
        final List<TestEditorAttributes> ordered = Arrays.stream(TestEditorAttributes.values()).toList();

        final String[] columns = buildColumns(ordered, TestEditorAttributes::getName);
        final List<String[]> rows = new ArrayList<>();

        int index = 1;
        for (final TestCaseDto tc : testCases) {
            final String[] row = new String[columns.length];
            row[0] = String.valueOf(index++);
            int c = 1;
            for (final TestEditorAttributes attr : ordered) {
                row[c++] = attr.getTestValueExtractor().execute(tc, p);
            }
            rows.add(row);
        }

        final JBTable table = buildTable(columns, rows, true);
        applyColumnVisibility(table, ordered, TestEditorAttributes::getName, attributes);
        return table;
    }

    public <E> void applyColumnVisibility(final JBTable table, final List<E> allValues, final java.util.function.Function<E, String> name, final Set<E> selected) {
        final TableColumnModel cm = table.getColumnModel();
        while (cm.getColumnCount() > 0) {
            cm.removeColumn(cm.getColumn(cm.getColumnCount() - 1));
        }

        cm.addColumn(columnFor(0, "#"));
        for (int i = 0; i < allValues.size(); i++) {
            final E attr = allValues.get(i);
            if (selected.contains(attr)) {
                cm.addColumn(columnFor(i + 1, name.apply(attr)));
            }
        }

        autoSizeColumns(table);
    }

    private TableColumn columnFor(final int modelIndex, final String header) {
        final TableColumn column = new TableColumn(modelIndex);
        column.setHeaderValue(header);
        return column;
    }

    private <E> String[] buildColumns(final List<E> attributes, final java.util.function.Function<E, String> name) {
        final List<String> columns = new ArrayList<>();
        columns.add("#");
        attributes.forEach(attr -> columns.add(name.apply(attr)));
        return columns.toArray(new String[0]);
    }

    private JBTable buildTable(final String[] columns, final List<String[]> rows, final boolean editable) {
        final DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(final int row, final int column) {
                return editable && column > 0;
            }
        };

        for (final String[] row : rows) {
            model.addRow(row);
        }

        final JBTable table = new JBTable(model);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JBTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        if (editable) {
            table.setDefaultEditor(Object.class, new GridCellEditor());
        }

        return table;
    }
}
