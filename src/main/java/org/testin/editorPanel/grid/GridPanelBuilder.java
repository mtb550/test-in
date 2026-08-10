package org.testin.editorPanel.grid;

import com.intellij.openapi.project.Project;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.testin.editorPanel.EditorColors;
import org.testin.enums.RunEditorAttributes;
import org.testin.enums.TestEditorAttributes;
import org.testin.logger.Logger;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.util.*;
import java.util.List;

public class GridPanelBuilder {

    static final int CELL_PADDING = 10;
    private static final int MAX_COL_WIDTH = 500;
    static final Color GRID_COLOR = JBColor.border();
    static final Color SELECTION_BACKGROUND = EditorColors.SELECTION_BACKGROUND;
    private static final Color EVEN_ROW_COLOR = new JBColor(Gray._245, Gray._60);
    private static final Color ODD_ROW_COLOR = new JBColor(Gray._230, Gray._45);
    private static final Border FIRST_CELL_SELECTION_BORDER = new SelectionCellBorder(true);
    private static final Border CELL_SELECTION_BORDER = new SelectionCellBorder(false);

    static Color rowColor(final int row) {
        return row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR;
    }

    public static void resizeToFont(final JBTable table) {
        final FontMetrics fm = table.getFontMetrics(table.getFont());
        table.setRowHeight(Math.max(fm.getHeight() + 4, 20));
        autoSizeColumns(table);
        updateRowHeights(table);
    }

    private static TableCellRenderer wrappingRenderer() {
        return new TableCellRenderer() {
            private final JTextArea textArea = new JTextArea();
            private final JPanel wrapper = new JPanel(new GridBagLayout());
            private final GridBagConstraints c = new GridBagConstraints();

            {
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setBorder(BorderFactory.createEmptyBorder(CELL_PADDING, CELL_PADDING, CELL_PADDING, CELL_PADDING));
                textArea.setOpaque(false);
                wrapper.setOpaque(true);
                c.gridx = 0;
                c.gridy = 0;
                c.weightx = 1.0;
                c.weighty = 1.0;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.anchor = GridBagConstraints.WEST;
            }

            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {

                textArea.setText(value == null ? "" : value.toString());
                textArea.setFont(table.getFont());
                textArea.setForeground(table.getForeground());
                final boolean isSelectedRow = table.getSelectedRow() == row;
                wrapper.setBackground(isSelectedRow ? SELECTION_BACKGROUND : rowColor(row));

                if (isSelected) {
                    // Keep the same insets as an unselected cell so selection never
                    // changes the cell width or causes text to wrap differently.
                    wrapper.setBorder(column == 0 ? FIRST_CELL_SELECTION_BORDER : CELL_SELECTION_BORDER);

                } else {
                    Border gridBorder = BorderFactory.createMatteBorder(0, 0, 1, 1, GRID_COLOR);
                    Border invisiblePadding = BorderFactory.createEmptyBorder(1, column == 0 ? 1 : 0, 0, 0);
                    wrapper.setBorder(BorderFactory.createCompoundBorder(invisiblePadding, gridBorder));
                }

                final int width = table.getColumnModel().getColumn(column).getWidth();
                textArea.setSize(new Dimension(width, Short.MAX_VALUE));
                wrapper.add(textArea, c);
                return wrapper;
            }
        };
    }

    private static void updateRowHeights(final JBTable table) {
        if (table.getRowCount() == 0) return;
        final int baseHeight = table.getRowHeight();
        for (int r = 0; r < table.getRowCount(); r++) {
            int maxHeight = baseHeight;
            for (int c = 0; c < table.getColumnCount(); c++) {
                // Measure the normal cell layout. Selection is a visual state and must not
                // change the row height when its blue border is applied.
                final TableCellRenderer renderer = table.getCellRenderer(r, c);
                final Component comp = renderer.getTableCellRendererComponent(
                        table, table.getValueAt(r, c), false, false, r, c);
                maxHeight = Math.max(maxHeight, comp.getPreferredSize().height);
            }
            table.setRowHeight(r, maxHeight);
        }
    }

    private static void addWheelScrollListener(final JBTable table) {
        table.addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(final MouseWheelEvent e) {
                if (e.isControlDown() || e.isMetaDown()) return;
                JBScrollPane scrollPane = getScrollPane(e.getComponent());
                if (scrollPane != null && e.getComponent() != scrollPane) {
                    MouseWheelEvent cloned = (MouseWheelEvent) SwingUtilities.convertMouseEvent(e.getComponent(), e, scrollPane);
                    scrollPane.dispatchEvent(cloned);
                    e.consume();
                }
            }
        });
    }

    private static void addColumnResizeListener(final JBTable table) {
        table.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            @Override
            public void columnMarginChanged(final javax.swing.event.ChangeEvent e) {
                updateRowHeights(table);
            }

            @Override
            public void columnAdded(final javax.swing.event.TableColumnModelEvent e) {
            }

            @Override
            public void columnRemoved(final javax.swing.event.TableColumnModelEvent e) {
            }

            @Override
            public void columnMoved(final javax.swing.event.TableColumnModelEvent e) {
                updateRowHeights(table);
            }

            @Override
            public void columnSelectionChanged(final javax.swing.event.ListSelectionEvent e) {
            }
        });
    }

    private static JBScrollPane getScrollPane(final Component component) {
        Component current = component;
        while (current != null) {
            if (current instanceof JBScrollPane) return (JBScrollPane) current;
            current = current.getParent();
        }
        return null;
    }

    public JBTable buildRunTable(final Project p, final List<TestCaseDto> testCases, final Set<RunEditorAttributes> attributes, final Map<UUID, TestRunItems> resultsMap) {
        return buildRunTable(p, testCases, attributes, resultsMap, 0);
    }

    private static void autoSizeColumns(final JBTable table) {
        final FontMetrics fm = table.getFontMetrics(table.getFont());
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
                final Object value = table.getValueAt(r, i);
                if (value != null) {
                    maxWidth = Math.max(maxWidth, fm.stringWidth(value.toString()));
                }
            }

            maxWidth += 2 * CELL_PADDING + 20;
            col.setPreferredWidth(Math.min(maxWidth, MAX_COL_WIDTH));
            tableTotalWidth += Math.min(maxWidth, MAX_COL_WIDTH);
        }

        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        table.setPreferredScrollableViewportSize(new Dimension(
                Math.min(tableTotalWidth, (int) (screenSize.width * 0.85)),
                Math.min(table.getRowHeight() * Math.max(3, table.getRowCount()), (int) (screenSize.height * 0.70))
        ));
    }

    public JBTable buildRunTable(final Project p, final List<TestCaseDto> testCases, final Set<RunEditorAttributes> attributes, final Map<UUID, TestRunItems> resultsMap, final int firstItemIndex) {
        Logger.debug("[GridPanelBuilder] buildRunTable: testCases=" + testCases.size() + ", attributes=" + attributes);
        final List<RunEditorAttributes> ordered = Arrays.stream(RunEditorAttributes.values()).toList();

        final String[] columns = buildColumns(ordered, RunEditorAttributes::getName);
        final List<String[]> rows = new ArrayList<>();

        int index = firstItemIndex + 1;
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
        return buildTestTable(p, testCases, attributes, 0);
    }

    public JBTable buildTestTable(final Project p, final List<TestCaseDto> testCases, final Set<TestEditorAttributes> attributes, final int firstItemIndex) {
        Logger.debug("[GridPanelBuilder] buildTestTable: testCases=" + testCases.size() + ", attributes=" + attributes);
        final List<TestEditorAttributes> ordered = Arrays.stream(TestEditorAttributes.values()).toList();

        final String[] columns = buildColumns(ordered, TestEditorAttributes::getName);
        final List<String[]> rows = new ArrayList<>();

        int index = firstItemIndex + 1;
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
        // JBTable's IntelliJ UI paints a rollover background over table rows.
        // The grid renderer owns all row colors, so use the standard table UI here.
        table.setUI(new BasicTableUI());
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JBTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setCellSelectionEnabled(true);
        table.setSelectionBackground(SELECTION_BACKGROUND);
        table.setSelectionForeground(table.getForeground());
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.setDefaultRenderer(Object.class, wrappingRenderer());
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBorder(BorderFactory.createLineBorder(GRID_COLOR, 1));
        table.setExpandableItemsEnabled(false);
        addColumnResizeListener(table);
        addWheelScrollListener(table);
        if (editable) {
            table.setDefaultEditor(Object.class, new GridCellEditor());
        }

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

    private record SelectionCellBorder(Insets insets) implements Border {
            private SelectionCellBorder(final boolean firstColumn) {
                this(new Insets(1, firstColumn ? 1 : 0, 1, 1));
            }

            @Override
            public Insets getBorderInsets(final Component component) {
                return insets;
            }

            @Override
            public boolean isBorderOpaque() {
                return false;
            }

            @Override
            public void paintBorder(final Component component, final Graphics graphics, final int x, final int y, final int width, final int height) {
                final Color previousColor = graphics.getColor();
                graphics.setColor(EditorColors.SELECTION_BORDER);
                graphics.drawRect(x, y, width - 1, height - 1);
                graphics.setColor(previousColor);
            }
        }
}
