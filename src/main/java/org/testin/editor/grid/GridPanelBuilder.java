package org.testin.editor.grid;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.EditorColors;
import org.testin.editor.Shared;
import org.testin.logger.Logger;
import org.testin.model.RunEditorAttributes;
import org.testin.model.TestEditorAttributes;
import org.testin.model.TestEditorAttributes.Can;
import org.testin.model.TestRunItems;
import org.testin.model.ToolBarAttribute;
import org.testin.model.dto.TestCaseDto;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.TableModelEvent;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntPredicate;
import java.util.function.ToIntFunction;

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

    /**
     * The grid lines an unselected cell draws, in its two forms - the first
     * column carries a pixel of padding on its left and no other column does.
     * <p>
     * Constants for the same reason the two selection borders above are. The
     * renderer built all three of these objects on every call, and the renderer
     * is called for every cell of every paint and again for every cell of every
     * height measurement - so a single re-measure of a 50-row page allocated two
     * thousand seven hundred borders that never differ (#168).
     * <p>
     * A {@link JBColor} inside a border resolves per paint, so one constant is
     * still correct in both themes.
     */
    private static final @NotNull Border FIRST_CELL_BORDER = cellBorder(1);
    private static final @NotNull Border CELL_BORDER = cellBorder(0);

    private static @NotNull Border cellBorder(final int leftPadding) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(1, leftPadding, 0, 0),
                BorderFactory.createMatteBorder(0, 0, 1, 1, GRID_COLOR));
    }
    /**
     * The model column ORDER occupies in both grids. A column carries its
     * attribute's ordinal as its model index, and ORDER is declared first in
     * both attribute enums - {@code AttributeOrderTest} pins that, because
     * nothing else would fail if a constant were declared above it.
     */
    private static final int ORDER_COLUMN = 0;

    static @NotNull Color rowColor(final int row) {
        return row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR;
    }

    public static void resizeToFont(final @NotNull JBTable table) {
        final @NotNull FontMetrics fm = table.getFontMetrics(table.getFont());
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

                final @NotNull String raw = Objects.toString(value, "");
                textArea.setText(raw);
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
                    wrapper.setBorder(column == 0 ? FIRST_CELL_BORDER : CELL_BORDER);
                }

                final int width = table.getColumnModel().getColumn(column).getWidth();
                textArea.setSize(new Dimension(width, Short.MAX_VALUE));
                wrapper.add(textArea, c);
                return wrapper;
            }
        };
    }

    /**
     * Every row, for the callers that changed something every row can feel - the
     * font, or the table itself.
     */
    private static void updateRowHeights(final @NotNull JBTable table) {
        updateRowHeights(table, 0, Integer.MAX_VALUE);
    }

    /**
     * The height of the rows between these two, measured by asking the renderer
     * to lay out every cell in them.
     * <p>
     * A range rather than the page, because that is what the callers know and
     * what they were throwing away. One cell edit used to re-measure all fifty
     * rows: at eighteen columns that is nine hundred cell layouts, each one
     * setting text, font, colors and three freshly built borders, for one row
     * that could have changed height (#168).
     * <p>
     * Clamped here rather than by the callers, so {@code Integer.MAX_VALUE}
     * means "to the end" and nobody has to know the row count to say it.
     */
    private static void updateRowHeights(final @NotNull JBTable table, final int firstRow, final int lastRow) {
        if (table.getRowCount() == 0) return;

        final int from = Math.max(0, firstRow);
        final int to = Math.min(lastRow, table.getRowCount() - 1);

        final int baseHeight = table.getRowHeight();
        for (int r = from; r <= to; r++) {
            int maxHeight = baseHeight;
            for (int c = 0; c < table.getColumnCount(); c++) {
                // Measure the normal cell layout. Selection is a visual state and must not
                // change the row height when its blue border is applied.
                final @NotNull TableCellRenderer renderer = table.getCellRenderer(r, c);
                final @NotNull Component comp = renderer.getTableCellRendererComponent(
                        table, table.getValueAt(r, c), false, false, r, c);
                maxHeight = Math.max(maxHeight, comp.getPreferredSize().height);
            }
            table.setRowHeight(r, maxHeight);
        }
    }

    /**
     * When row heights are re-measured, and for which rows.
     * <p>
     * One owner, because four things ask and they used to answer differently: an
     * edit coalesced its own bursts through a flag of its own, and the three
     * column-model events each called straight through. A divider drag fires
     * {@code columnMarginChanged} per pixel of movement, so dragging one column
     * an inch was a full page of cell layouts per pixel (#168).
     * <p>
     * The range accumulates across the burst: two cells edited in one gesture
     * are measured once, over both their rows.
     */
    private static final class RowHeights {

        private final @NotNull JBTable table;
        private final @NotNull AtomicBoolean pending = new AtomicBoolean();

        /**
         * The rows the burst has asked for so far. Inverted when nothing is
         * pending, so the first request sets both ends.
         */
        private int from = Integer.MAX_VALUE;
        private int to = -1;

        private RowHeights(final @NotNull JBTable table) {
            this.table = table;
        }

        /**
         * Every row: a column appeared, moved or changed width, so anything on
         * the page can wrap differently now.
         */
        private void scheduleAll() {
            schedule(0, Integer.MAX_VALUE);
        }

        private void schedule(final int firstRow, final int lastRow) {
            from = Math.min(from, firstRow);
            to = Math.max(to, lastRow);

            // Already queued: the range above is what the queued pass will read,
            // so this request is in it and needs nothing of its own.
            if (!pending.compareAndSet(false, true)) return;

            SwingUtilities.invokeLater(() -> {
                final int first = from;
                final int last = to;
                from = Integer.MAX_VALUE;
                to = -1;
                pending.set(false);

                updateRowHeights(table, first, last);
            });
        }
    }

    /**
     * Re-measures row heights after cell values change (edit, paste, cut), so a
     * value that now wraps to more lines becomes fully visible immediately.
     * Coalesced via invokeLater: a block paste triggers one re-measure, not one per cell.
     */
    private static void installAutoRowHeight(final @NotNull DefaultTableModel model, final @NotNull RowHeights rowHeights) {
        model.addTableModelListener(e -> {
            if (e.getType() != TableModelEvent.UPDATE) return;

            // The event says which rows: one edited cell is one row, and a
            // block paste is the rows it covered.
            //
            // A negative row is HEADER_ROW, which is how the model says the
            // structure changed rather than a cell - every row, then, because
            // any of them can wrap differently now. Read as a range it would be
            // empty, and the pass would silently do nothing.
            final int last = e.getLastRow() < 0 ? Integer.MAX_VALUE : e.getLastRow();

            rowHeights.schedule(Math.max(0, e.getFirstRow()), last);
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

    /**
     * Where this column's width is remembered, and empty for a table carrying no
     * kind - one with nothing to remember it under.
     * <p>
     * The save and the read both ask here, so a width cannot be written under a
     * key the reader would not look at. Each used to build the key itself after
     * checking the kind for null, which is the same question asked twice.
     */
    private static @NotNull Optional<String> widthKey(final @NotNull JBTable table, final @NotNull TableColumn column) {
        return Optional.ofNullable(table.getClientProperty(GRID_KIND_KEY))
                .map(kind -> "testin.grid.colWidth." + kind + "." + column.getHeaderValue());
    }

    private static void addColumnResizeListener(final @NotNull JBTable table, final @NotNull RowHeights rowHeights) {
        table.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            @Override
            public void columnMarginChanged(final javax.swing.event.ChangeEvent e) {
                // getResizingColumn() is non-null only during a user drag-resize,
                // so programmatic auto-sizing never overwrites the saved widths.
                Optional.ofNullable(table.getTableHeader())
                        .map(JTableHeader::getResizingColumn)
                        .ifPresent(resizing -> widthKey(table, resizing).ifPresent(key ->
                                PropertiesComponent.getInstance().setValue(key, resizing.getWidth(), -1)));

                rowHeights.scheduleAll();
            }

            @Override
            public void columnAdded(final javax.swing.event.TableColumnModelEvent e) {
            }

            @Override
            public void columnRemoved(final javax.swing.event.TableColumnModelEvent e) {
            }

            @Override
            public void columnMoved(final javax.swing.event.TableColumnModelEvent e) {
                rowHeights.scheduleAll();
            }

            @Override
            public void columnSelectionChanged(final javax.swing.event.ListSelectionEvent e) {
            }
        });
    }

    /**
     * Puts the grid's selection back on whichever case the list has selected,
     * in the column the tester was last in.
     * <p>
     * Called after every rebuild, by both editors, which each wrote it out. The
     * column is checked against the rebuilt table rather than trusted: a grid
     * rebuilt with fewer columns would otherwise be asked to select one it no
     * longer has.
     */
    public static void restoreSelection(final @NotNull JBTable table, final @NotNull JBList<TestCaseDto> list, final @NotNull List<TestCaseDto> pageItems, final int columnToRestore) {
        final int selectedRow = pageItems.indexOf(list.getSelectedValue());
        if (selectedRow < 0) return;

        final int column = columnToRestore >= 0 && columnToRestore < table.getColumnCount() ? columnToRestore : 0;

        table.changeSelection(selectedRow, column, false, false);
        table.scrollRectToVisible(table.getCellRect(selectedRow, column, true));
    }

    /**
     * Sizes every column to its content, capped, and sets the viewport to fit.
     * <p>
     * Public because the import and export preview had a copy of this without
     * the cap and without the early exit. One long Steps or Description value
     * made that column as wide as the text, so the tester scrolled sideways past
     * one enormous column to reach the checkbox - and the measuring loop
     * prepared a renderer for every cell of every row on the UI thread, roughly
     * 5,500 preparations for a 550-case sheet before the dialog painted.
     * <p>
     * Safe on a table that is not a grid: the remembered widths are read under a
     * key built from the table's kind, and a table carrying no kind has none.
     */
    public static void autoSizeColumns(final @NotNull JBTable table) {
        final @NotNull FontMetrics fm = table.getFontMetrics(table.getFont());
        int tableTotalWidth = 0;
        for (int i = 0; i < table.getColumnCount(); i++) {
            final @NotNull TableColumn col = table.getColumnModel().getColumn(i);

            // A width the user set by dragging wins over auto-sizing,
            // so refreshes and page changes keep the chosen layout.
            final int savedWidth = widthKey(table, col)
                    .map(key -> PropertiesComponent.getInstance().getInt(key, -1))
                    .orElse(-1);
            if (savedWidth > 0) {
                col.setPreferredWidth(savedWidth);
                tableTotalWidth += savedWidth;
                continue;
            }

            // A column with no renderer of its own is drawn by the header's, which
            // is what the table would have used anyway.
            final @NotNull TableCellRenderer headerRenderer = Optional.ofNullable(col.getHeaderRenderer())
                    .orElseGet(() -> table.getTableHeader().getDefaultRenderer());

            final @NotNull Component headerComp = headerRenderer.getTableCellRendererComponent(
                    table, col.getHeaderValue(), false, false, 0, i);
            int maxWidth = headerComp.getPreferredSize().width;

            // The width is capped below, so once a row has pushed it past the cap
            // no later row can change the answer. A Description column reaches
            // that on its first long row, and measuring the rest of the page is
            // pure cost - this runs again on every attribute ticked.
            final int capBeforePadding = MAX_COL_WIDTH - (2 * CELL_PADDING + 20);

            for (int r = 0; r < table.getRowCount() && maxWidth < capBeforePadding; r++) {
                // An empty cell measures zero and so never widens the column.
                maxWidth = Math.max(maxWidth, fm.stringWidth(Objects.toString(table.getValueAt(r, i), "")));
            }

            maxWidth += 2 * CELL_PADDING + 20;
            col.setPreferredWidth(Math.min(maxWidth, MAX_COL_WIDTH));
            tableTotalWidth += Math.min(maxWidth, MAX_COL_WIDTH);
        }

        final @NotNull Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        table.setPreferredScrollableViewportSize(new Dimension(
                Math.min(tableTotalWidth, (int) (screenSize.width * 0.85)),
                Math.min(table.getRowHeight() * Math.max(3, table.getRowCount()), (int) (screenSize.height * 0.70))
        ));
    }

    private static void installEnterToEdit(final @NotNull JBTable table) {
        final @NotNull KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        final @NotNull Action startEditing = new AbstractAction() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent event) {
                final int row = table.getSelectedRow();
                final int column = table.getSelectedColumn();
                if (row < 0 || column < 0 || !table.isCellEditable(row, column)) return;
                if (table.editCellAt(row, column)) {
                    Optional.ofNullable(table.getEditorComponent()).ifPresent(Component::requestFocusInWindow);
                }
            }
        };
        table.getInputMap(JComponent.WHEN_FOCUSED).put(enter, "startEditing");
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(enter, "startEditing");
        table.getActionMap().put("startEditing", startEditing);
    }

    /**
     * Whether a model column is the order column: the row number, the one column
     * that is never edited, and the target of the two gestures that are not edits
     * - clicking it selects the whole row, ENTER and double-click open details.
     */
    public static boolean isOrderColumn(final int modelColumn) {
        return modelColumn == ORDER_COLUMN;
    }

    /**
     * The same question asked of a view column, which is what a mouse position or
     * a selection gives. False for every column while Order is unticked, because
     * nothing on screen maps to its model index then - so the gestures that need
     * it go quiet together rather than one of them acting on another column.
     */
    public static boolean isOrderColumn(final @NotNull JTable table, final int viewColumn) {
        return viewColumn >= 0 && isOrderColumn(table.convertColumnIndexToModel(viewColumn));
    }

    public @NotNull JBTable buildRunTable(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases, final @NotNull Set<RunEditorAttributes> attributes, final @NotNull Map<UUID, TestRunItems> resultsMap, final @NotNull ToIntFunction<TestCaseDto> position) {
        Logger.debug("[GridPanelBuilder] buildRunTable: testCases=" + testCases.size() + ", attributes=" + attributes);
        final @NotNull List<RunEditorAttributes> ordered = Arrays.stream(RunEditorAttributes.values()).toList();

        final String @NotNull[] columns = buildColumns(ordered);
        final @NotNull List<String[]> rows = new ArrayList<>();

        for (final TestCaseDto tc : testCases) {
            // Never skip rows: callers map grid rows back to testCases by index,
            // so a dropped row would make every following row act on the wrong test case.
            final @NotNull TestRunItems runItem = Optional.ofNullable(resultsMap.get(tc.getId()))
                    .orElseGet(() -> TestRunItems.builder().id(tc.getId()).tc(tc).build());

            final String @NotNull[] row = new String[columns.length];
            final int rowNumber = position.applyAsInt(tc);

            for (int c = 0; c < ordered.size(); c++) {
                final @NotNull RunEditorAttributes attr = ordered.get(c);

                // ORDER is the one value the model cannot answer - it is the case's
                // place in its set, which no run item carries. Counted on the page
                // until a filter proved that wrong (#163). Recognized by
                // the constant rather than by the column number, so moving ORDER
                // within the enum moves its column and nothing else.
                row[c] = attr == RunEditorAttributes.ORDER
                        ? String.valueOf(rowNumber)
                        : attr.getRunValueExtractor().execute(runItem, p);
            }
            rows.add(row);
        }

        // Which columns can be typed into is the attribute's own declaration, the
        // same way the test grid asks its attributes (#74).
        final @NotNull JBTable table = buildTable(columns, rows,
                column -> ordered.get(column).isEdited(), "run");
        applyColumnVisibility(table, RunEditorAttributes.class, attributes);
        return table;
    }

    public @NotNull JBTable buildTestTable(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases, final @NotNull Set<TestEditorAttributes> attributes, final @NotNull ToIntFunction<TestCaseDto> position) {
        Logger.debug("[GridPanelBuilder] buildTestTable: testCases=" + testCases.size() + ", attributes=" + attributes);
        final @NotNull List<TestEditorAttributes> ordered = Arrays.stream(TestEditorAttributes.values()).toList();

        final String @NotNull[] columns = buildColumns(ordered);
        final @NotNull List<String[]> rows = new ArrayList<>();

        for (final TestCaseDto tc : testCases) {
            final String @NotNull[] row = new String[columns.length];
            final int rowNumber = position.applyAsInt(tc);

            for (int c = 0; c < ordered.size(); c++) {
                final @NotNull TestEditorAttributes attr = ordered.get(c);

                // ORDER is the one value the model cannot answer - it is the case's
                // place in its set, which no test case carries. Counted on the page
                // until a filter proved that wrong (#163). Recognized by
                // the constant rather than by the column number, so moving ORDER
                // within the enum moves its column and nothing else.
                row[c] = attr == TestEditorAttributes.ORDER
                        ? String.valueOf(rowNumber)
                        : attr.gridValue(p, tc);
            }
            rows.add(row);
        }

        final @NotNull JBTable table = buildTable(columns, rows, column -> ordered.get(column).can(Can.EDIT), "test");
        applyColumnVisibility(table, TestEditorAttributes.class, attributes);
        return table;
    }

    public <E extends Enum<E> & ToolBarAttribute> void applyColumnVisibility(final @NotNull JBTable table, final @NotNull Class<E> attributes, final @NotNull Set<E> selected) {
        final @NotNull TableColumnModel cm = table.getColumnModel();
        while (cm.getColumnCount() > 0) {
            cm.removeColumn(cm.getColumn(cm.getColumnCount() - 1));
        }

        final E @NotNull[] allValues = attributes.getEnumConstants();
        for (int i = 0; i < allValues.length; i++) {
            final @NotNull E attr = allValues[i];
            if (selected.contains(attr)) {
                cm.addColumn(columnFor(i, attr.getName()));
            }
        }

        autoSizeColumns(table);
    }

    private @NotNull TableColumn columnFor(final int modelIndex, final @NotNull String header) {
        final @NotNull TableColumn column = new TableColumn(modelIndex);
        column.setHeaderValue(header);
        return column;
    }

    private @NotNull JBTable buildTable(final String @NotNull [] columns, final @NotNull List<String[]> rows, final @NotNull IntPredicate columnEditable, final @NotNull String kind) {
        final @NotNull DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(final int row, final int column) {
                return columnEditable.test(column);
            }
        };

        for (final String[] row : rows) {
            model.addRow(row);
        }

        final @NotNull JBTable table = new JBTable(model) {
            /**
             * The grid renderer owns every row color. JBTable tints the hovered
             * row after the renderer has run, so the color is restored here -
             * removing the hover listener and swapping the UI were not enough on
             * their own (issue: hover background in grid view).
             */
            @Override
            public @NotNull Component prepareRenderer(final @NotNull TableCellRenderer renderer, final int row, final int column) {
                final @NotNull Component component = super.prepareRenderer(renderer, row, column);
                component.setBackground(isCellSelected(row, column) ? SELECTION_BACKGROUND : rowColor(row));
                return component;
            }
        };
        table.putClientProperty(GRID_KIND_KEY, kind);
        // The IntelliJ table UI paints a rollover background over table rows.
        // The grid renderer owns all row colors, so use the standard table UI here.
        table.setUI(new BasicTableUI());
        // The hover listener JBTable attaches in its constructor is left where it
        // is. It used to be detached here, through @ApiStatus.Experimental API,
        // because swapping the UI alone did not stop the tint - but prepareRenderer
        // above now sets every cell's background after super has run, which is
        // where JBTable applies that tint, so the color it produces is overwritten
        // for every cell on every paint. Detaching it was belt to a brace that
        // already holds, and the platform's own switch for this
        // (RenderingUtil.PAINT_HOVERED_BACKGROUND) is experimental too, so there
        // was nothing stable to move to (#66).
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JBTable.AUTO_RESIZE_OFF);
        // Excel/DataGrip-style selection and clipboard (multi-cell selection,
        // row selection via the order column, TSV copy/cut/paste).
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
        final @NotNull RowHeights rowHeights = new RowHeights(table);
        addColumnResizeListener(table, rowHeights);
        addWheelScrollListener(table);
        installAutoRowHeight(model, rowHeights);
        // Installed either way: a cell the model refuses to edit never reaches an
        // editor, so there is no second place deciding what is editable.
        table.setDefaultEditor(Object.class, new GridCellEditor());

        return table;
    }

    private String @NotNull [] buildColumns(final @NotNull List<? extends ToolBarAttribute> attributes) {
        return attributes.stream().map(ToolBarAttribute::getName).toArray(String[]::new);
    }

}
