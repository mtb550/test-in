package org.testin.editorPanel.grid;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.hover.TableHoverListener;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editorPanel.EditorColors;
import org.testin.editorPanel.Shared;
import org.testin.enums.RunEditorAttributes;
import org.testin.enums.TestEditorAttributes;
import org.testin.logger.Logger;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.TableModelEvent;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class GridPanelBuilder {

    static final int CELL_PADDING = 10;
    static final @NotNull Color GRID_COLOR = JBColor.border();
    static final @NotNull Color SELECTION_BACKGROUND = EditorColors.SELECTION_BACKGROUND;
    private static final int MAX_COL_WIDTH = 500;
    /**
     * Client property holding the table kind ("test"/"run"), used to key the
     * persisted user column widths so they survive grid rebuilds and restarts.
     */
    private static final @NotNull String GRID_KIND_KEY = "testin.grid.kind";
    private static final @NotNull Color EVEN_ROW_COLOR = new JBColor(Gray._245, Gray._60);
    private static final @NotNull Color ODD_ROW_COLOR = new JBColor(Gray._230, Gray._45);
    private static final @NotNull Border FIRST_CELL_SELECTION_BORDER = new SelectionCellBorder(true);
    private static final @NotNull Border CELL_SELECTION_BORDER = new SelectionCellBorder(false);

    static @NotNull Color rowColor(final int row) {
        return row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR;
    }

    public static void resizeToFont(final @NotNull JBTable table) {
        final FontMetrics fm = table.getFontMetrics(table.getFont());
        table.setRowHeight(Math.max(fm.getHeight() + 4, 20));
        autoSizeColumns(table);
        updateRowHeights(table);
    }

    private static @NotNull TableCellRenderer wrappingRenderer() {
        return new TableCellRenderer() {
            private final @NotNull JTextArea textArea = new JTextArea();
            private final @NotNull JPanel wrapper = new JPanel(new GridBagLayout());
            private final @NotNull GridBagConstraints c = new GridBagConstraints();

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
            public @NotNull Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {

                textArea.setText(value == null ? "" : value.toString());
                textArea.setFont(table.getFont());
                textArea.setForeground(table.getForeground());
                // Per-cell selection background (multi-interval selection: only the
                // cells inside the selection are highlighted, like Excel/DataGrip).
                wrapper.setBackground(isSelected ? SELECTION_BACKGROUND : rowColor(row));

                if (isSelected) {
                    // Keep the same insets as an unselected cell so selection never
                    // changes the cell width or causes text to wrap differently.
                    wrapper.setBorder(column == 0 ? FIRST_CELL_SELECTION_BORDER : CELL_SELECTION_BORDER);

                } else {
                    final Border gridBorder = BorderFactory.createMatteBorder(0, 0, 1, 1, GRID_COLOR);
                    final Border invisiblePadding = BorderFactory.createEmptyBorder(1, column == 0 ? 1 : 0, 0, 0);
                    wrapper.setBorder(BorderFactory.createCompoundBorder(invisiblePadding, gridBorder));
                }

                final int width = table.getColumnModel().getColumn(column).getWidth();
                textArea.setSize(new Dimension(width, Short.MAX_VALUE));
                wrapper.add(textArea, c);
                return wrapper;
            }
        };
    }

    private static void updateRowHeights(final @NotNull JBTable table) {
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

    /**
     * Re-measures row heights after cell values change (edit, paste, cut), so a
     * value that now wraps to more lines becomes fully visible immediately.
     * Coalesced via invokeLater: a block paste triggers one re-measure, not one per cell.
     */
    private static void installAutoRowHeight(final @NotNull JBTable table, final @NotNull DefaultTableModel model) {
        final AtomicBoolean pending = new AtomicBoolean();
        model.addTableModelListener(e -> {
            if (e.getType() != TableModelEvent.UPDATE) return;
            if (pending.compareAndSet(false, true)) {
                SwingUtilities.invokeLater(() -> {
                    pending.set(false);
                    updateRowHeights(table);
                });
            }
        });
    }

    private static void addWheelScrollListener(final @NotNull JBTable table) {
        table.addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(final MouseWheelEvent e) {
                Shared.forwardWheelToScrollPane(e);
            }
        });
    }

    private static @NotNull String widthKey(final @NotNull Object kind, final @Nullable Object header) {
        return "testin.grid.colWidth." + kind + "." + header;
    }

    private static void addColumnResizeListener(final @NotNull JBTable table) {
        table.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            @Override
            public void columnMarginChanged(final javax.swing.event.ChangeEvent e) {
                // getResizingColumn() is non-null only during a user drag-resize,
                // so programmatic auto-sizing never overwrites the saved widths.
                final TableColumn resizing = table.getTableHeader() != null
                        ? table.getTableHeader().getResizingColumn() : null;
                final Object kind = table.getClientProperty(GRID_KIND_KEY);
                if (resizing != null && kind != null) {
                    PropertiesComponent.getInstance()
                            .setValue(widthKey(kind, resizing.getHeaderValue()), resizing.getWidth(), -1);
                }

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

    private static void autoSizeColumns(final @NotNull JBTable table) {
        final FontMetrics fm = table.getFontMetrics(table.getFont());
        final Object kind = table.getClientProperty(GRID_KIND_KEY);
        int tableTotalWidth = 0;
        for (int i = 0; i < table.getColumnCount(); i++) {
            final TableColumn col = table.getColumnModel().getColumn(i);

            // A width the user set by dragging wins over auto-sizing,
            // so refreshes and page changes keep the chosen layout.
            final int savedWidth = kind != null
                    ? PropertiesComponent.getInstance().getInt(widthKey(kind, col.getHeaderValue()), -1)
                    : -1;
            if (savedWidth > 0) {
                col.setPreferredWidth(savedWidth);
                tableTotalWidth += savedWidth;
                continue;
            }

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

    private static void installEnterToEdit(final @NotNull JBTable table) {
        final KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        final Action startEditing = new AbstractAction() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent event) {
                final int row = table.getSelectedRow();
                final int column = table.getSelectedColumn();
                if (row < 0 || column < 0 || !table.isCellEditable(row, column)) return;
                if (table.editCellAt(row, column)) {
                    final Component editor = table.getEditorComponent();
                    if (editor != null) editor.requestFocusInWindow();
                }
            }
        };
        table.getInputMap(JComponent.WHEN_FOCUSED).put(enter, "startEditing");
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(enter, "startEditing");
        table.getActionMap().put("startEditing", startEditing);
    }

    public @NotNull JBTable buildRunTable(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases, final @NotNull Set<RunEditorAttributes> attributes, final @NotNull Map<UUID, TestRunItems> resultsMap, final int firstItemIndex) {
        Logger.debug("[GridPanelBuilder] buildRunTable: testCases=" + testCases.size() + ", attributes=" + attributes);
        final List<RunEditorAttributes> ordered = Arrays.stream(RunEditorAttributes.values()).toList();

        final String[] columns = buildColumns(ordered, RunEditorAttributes::getName);
        final List<String[]> rows = new ArrayList<>();

        int index = firstItemIndex + 1;
        for (final TestCaseDto tc : testCases) {
            // Never skip rows: callers map grid rows back to testCases by index,
            // so a dropped row would make every following row act on the wrong test case.
            TestRunItems runItem = resultsMap.get(tc.getId());
            if (runItem == null) {
                runItem = TestRunItems.builder().id(tc.getId()).tc(tc).build();
            }

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

    public @NotNull JBTable buildTestTable(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases, final @NotNull Set<TestEditorAttributes> attributes, final int firstItemIndex) {
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
                row[c++] = attr.gridValue(p, tc);
            }
            rows.add(row);
        }

        final JBTable table = buildTable(columns, rows, true);
        applyColumnVisibility(table, ordered, TestEditorAttributes::getName, attributes);
        return table;
    }

    public <E> void applyColumnVisibility(final @NotNull JBTable table, final @NotNull List<E> allValues, final @NotNull Function<E, String> name, final @NotNull Set<E> selected) {
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

    private @NotNull TableColumn columnFor(final int modelIndex, final @NotNull String header) {
        final TableColumn column = new TableColumn(modelIndex);
        column.setHeaderValue(header);
        return column;
    }

    private @NotNull JBTable buildTable(final String @NotNull [] columns, final @NotNull List<String[]> rows, final boolean editable) {
        final DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(final int row, final int column) {
                return editable && column > 0;
            }
        };

        for (final String[] row : rows) {
            model.addRow(row);
        }

        final JBTable table = new JBTable(model) {
            /**
             * The grid renderer owns every row color. JBTable tints the hovered
             * row after the renderer has run, so the color is restored here -
             * removing the hover listener and swapping the UI were not enough on
             * their own (issue: hover background in grid view).
             */
            @Override
            public @NotNull Component prepareRenderer(final @NotNull TableCellRenderer renderer, final int row, final int column) {
                final Component component = super.prepareRenderer(renderer, row, column);
                component.setBackground(isCellSelected(row, column) ? SELECTION_BACKGROUND : rowColor(row));
                return component;
            }
        };
        table.putClientProperty(GRID_KIND_KEY, editable ? "test" : "run");
        // The IntelliJ table UI paints a rollover background over table rows.
        // The grid renderer owns all row colors, so use the standard table UI here.
        table.setUI(new BasicTableUI());
        // Swapping the UI is not enough: JBTable also attaches a hover listener in
        // its constructor, which keeps tracking the hovered row and repainting it.
        TableHoverListener.DEFAULT.removeFrom(table);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JBTable.AUTO_RESIZE_OFF);
        // Excel/DataGrip-style selection and clipboard (multi-cell selection,
        // row selection via the "#" column, TSV copy/cut/paste).
        GridExcelBehavior.install(table);
        table.setSelectionBackground(SELECTION_BACKGROUND);
        table.setSelectionForeground(table.getForeground());
        installEnterToEdit(table);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.setDefaultRenderer(Object.class, wrappingRenderer());
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBorder(BorderFactory.createLineBorder(GRID_COLOR, 1));
        table.setExpandableItemsEnabled(false);
        addColumnResizeListener(table);
        addWheelScrollListener(table);
        installAutoRowHeight(table, model);
        if (editable) {
            table.setDefaultEditor(Object.class, new GridCellEditor());
        }

        return table;
    }

    private <E> String @NotNull [] buildColumns(final @NotNull List<E> attributes, final @NotNull Function<E, String> name) {
        final List<String> columns = new ArrayList<>();
        columns.add("#");
        attributes.forEach(attr -> columns.add(name.apply(attr)));
        return columns.toArray(new String[0]);
    }

}
