package org.testin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.Group;
import org.testin.pojo.Priority;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class ExcelPreviewDialog extends DialogWrapper {
    private final Map<String, List<TestCaseDto>> originalSheetsData;
    private final Map<String, DefaultTableModel> tableModelsMap = new LinkedHashMap<>();
    private final Project project;

    private final List<TestEditorAttributes> importAttributes = Arrays.stream(TestEditorAttributes.values())
            .filter(TestEditorAttributes::isImportValue)
            .toList();

    public ExcelPreviewDialog(final @Nullable Project project, final Map<String, List<TestCaseDto>> sheetsData) {
        super(project, true);
        this.project = project;
        this.originalSheetsData = sheetsData;

        setTitle("Preview & Select Excel Import");
        setOKButtonText("Import Selected");
        setCancelButtonText("Cancel");

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JBTabbedPane tabbedPane = new JBTabbedPane();

        List<String> columnNames = new ArrayList<>();
        columnNames.add("");
        columnNames.add("#");
        for (TestEditorAttributes attr : importAttributes) {
            columnNames.add(attr.getName());
        }
        String[] columns = columnNames.toArray(new String[0]);

        for (Map.Entry<String, List<TestCaseDto>> entry : originalSheetsData.entrySet()) {
            String sheetName = entry.getKey();
            List<TestCaseDto> testCases = entry.getValue();

            DefaultTableModel model = new DefaultTableModel(columns, 0) {
                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    if (columnIndex == 0) return Boolean.class;
                    return String.class;
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

            model.addTableModelListener(new TableModelListener() {
                boolean isUpdating = false;

                @Override
                public void tableChanged(TableModelEvent e) {
                    if (isUpdating) return;

                    if (e.getType() == TableModelEvent.UPDATE) {
                        int row = e.getFirstRow();
                        int col = e.getColumn();

                        if (row >= 0 && col >= 2) {
                            isUpdating = true;
                            try {
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
            });

            tableModelsMap.put(sheetName, model);

            JBTable table = new JBTable(model);
            table.setFillsViewportHeight(true);

            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

            TableColumn importColumn = table.getColumnModel().getColumn(0);

            JCheckBox headerCheckbox = new JCheckBox();
            headerCheckbox.setSelected(true);
            headerCheckbox.setHorizontalAlignment(SwingConstants.CENTER);
            headerCheckbox.setToolTipText("Select All / Deselect All");

            importColumn.setHeaderRenderer((t, value, isSelected, hasFocus, row, column) -> {
                JTableHeader header = t.getTableHeader();
                headerCheckbox.setBackground(header.getBackground());
                headerCheckbox.setForeground(header.getForeground());
                headerCheckbox.setFont(header.getFont());
                headerCheckbox.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
                return headerCheckbox;
            });

            table.getTableHeader().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int col = table.columnAtPoint(e.getPoint());
                    if (col == 0) {
                        boolean newState = !headerCheckbox.isSelected();
                        headerCheckbox.setSelected(newState);

                        for (int i = 0; i < model.getRowCount(); i++) {
                            model.setValueAt(newState, i, 0);
                        }
                        table.getTableHeader().repaint();
                    }
                }
            });

            try {
                TableColumn priorityCol = table.getColumn("Priority");
                ComboBox<String> priorityBox = new ComboBox<>();
                for (Priority p : Priority.values()) {
                    priorityBox.addItem(p.getName());
                }
                priorityCol.setCellEditor(new DefaultCellEditor(priorityBox));
            } catch (IllegalArgumentException ignored) {
            }

            try {
                TableColumn groupCol = table.getColumn("Group");
                groupCol.setCellEditor(new GroupMultiSelectEditor(project));
            } catch (IllegalArgumentException ignored) {
            }

            int tableTotalWidth = 0;
            for (int i = 0; i < table.getColumnCount(); i++) {
                TableColumn col = table.getColumnModel().getColumn(i);
                int maxWidth = 0;

                TableCellRenderer headerRenderer = col.getHeaderRenderer();
                if (headerRenderer == null) {
                    headerRenderer = table.getTableHeader().getDefaultRenderer();
                }
                Component headerComp = headerRenderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, i);
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

            JBScrollPane scrollPane = new JBScrollPane(table);
            tabbedPane.addTab(sheetName, scrollPane);
        }

        panel.add(tabbedPane, BorderLayout.CENTER);

        return panel;
    }

    public Map<String, List<TestCaseDto>> getSelectedTestCasesBySheet() {
        Map<String, List<TestCaseDto>> selectedCasesBySheet = new LinkedHashMap<>();

        for (Map.Entry<String, List<TestCaseDto>> entry : originalSheetsData.entrySet()) {
            String sheetName = entry.getKey();
            List<TestCaseDto> allCasesInSheet = entry.getValue();
            DefaultTableModel model = tableModelsMap.get(sheetName);

            List<TestCaseDto> selectedCases = new ArrayList<>();
            if (model != null) {
                for (int row = 0; row < model.getRowCount(); row++) {
                    Boolean isSelected = (Boolean) model.getValueAt(row, 0);
                    if (Boolean.TRUE.equals(isSelected)) {
                        selectedCases.add(allCasesInSheet.get(row));
                    }
                }
            }
            if (!selectedCases.isEmpty()) {
                selectedCasesBySheet.put(sheetName, selectedCases);
            }
        }
        return selectedCasesBySheet;
    }

    private static class GroupMultiSelectEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button = new JButton();
        private String currentValue = "";

        public GroupMultiSelectEditor(final Project project) {
            button.setBorderPainted(false);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setBackground(UIManager.getColor("Table.selectionBackground"));
            button.setForeground(UIManager.getColor("Table.selectionForeground"));

            button.addActionListener(e -> {
                GroupSelectionDialog dialog = new GroupSelectionDialog(project, currentValue);
                if (dialog.showAndGet()) {
                    currentValue = dialog.getSelectedGroupsStr();
                }
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentValue = value != null ? value.toString() : "";
            button.setText(currentValue);
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return currentValue;
        }
    }

    private static class GroupSelectionDialog extends DialogWrapper {
        private final JBList<String> list;

        public GroupSelectionDialog(Project project, String currentSelection) {
            super(project, true);
            setTitle("Select Groups");

            DefaultListModel<String> model = new DefaultListModel<>();
            for (Group g : Group.values()) {
                model.addElement(g.getName());
            }
            list = new JBList<>(model);
            list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

            if (currentSelection != null && !currentSelection.isBlank()) {
                List<String> selectedList = Arrays.stream(currentSelection.split(","))
                        .map(String::trim).toList();

                List<Integer> indices = new ArrayList<>();
                for (int i = 0; i < model.getSize(); i++) {
                    if (selectedList.contains(model.getElementAt(i))) {
                        indices.add(i);
                    }
                }
                list.setSelectedIndices(indices.stream().mapToInt(i -> i).toArray());
            }

            init();
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JBScrollPane(list), BorderLayout.CENTER);
            return panel;
        }

        public String getSelectedGroupsStr() {
            return String.join(", ", list.getSelectedValuesList());
        }
    }
}