package org.testin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.dto.TestCaseDto;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// todo, to be enhanced, make it dynamic for re-use, add horizontal scroll, add remain columns, make all cells editable.
public class ExcelPreviewDialog extends DialogWrapper {
    private final Map<String, List<TestCaseDto>> originalSheetsData;
    private final Map<String, DefaultTableModel> tableModelsMap = new LinkedHashMap<>();

    public ExcelPreviewDialog(@Nullable final Project project, final Map<String, List<TestCaseDto>> sheetsData) {
        super(project, true);
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
        panel.setPreferredSize(new Dimension(900, 450));

        JBTabbedPane tabbedPane = new JBTabbedPane();

        String[] columns = {"", "#", "Description", "Priority", "Expected Result", "Steps Count"};

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
                    return column == 0 || column == 2;
                }
            };

            int index = 1;
            for (TestCaseDto tc : testCases) {
                String priority = tc.getPriority().name();
                String stepsCount = tc.getSteps().size() + " Steps";

                model.addRow(new Object[]{
                        Boolean.TRUE,
                        String.valueOf(index++),
                        tc.getDescription(),
                        priority,
                        tc.getExpectedResult(),
                        stepsCount
                });
            }

            model.addTableModelListener(e -> {
                if (e.getType() == TableModelEvent.UPDATE) {
                    int row = e.getFirstRow();
                    int col = e.getColumn();

                    if (row >= 0 && col == 2) {
                        String updatedDescription = (String) model.getValueAt(row, col);
                        testCases.get(row).setDescription(updatedDescription);
                    }
                }
            });

            tableModelsMap.put(sheetName, model);

            JBTable table = new JBTable(model);
            table.setFillsViewportHeight(true);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

            TableColumn importColumn = table.getColumnModel().getColumn(0);
            importColumn.setMaxWidth(60);
            importColumn.setMinWidth(60);

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

            table.getColumnModel().getColumn(1).setMaxWidth(50);

            JBScrollPane scrollPane = new JBScrollPane(table);
            tabbedPane.addTab(sheetName, scrollPane);
        }

        panel.add(tabbedPane, BorderLayout.CENTER);

        return panel;
    }

    public List<TestCaseDto> getSelectedTestCases() {
        List<TestCaseDto> selectedCases = new ArrayList<>();

        for (Map.Entry<String, List<TestCaseDto>> entry : originalSheetsData.entrySet()) {
            String sheetName = entry.getKey();
            List<TestCaseDto> allCasesInSheet = entry.getValue();
            DefaultTableModel model = tableModelsMap.get(sheetName);

            if (model != null) {
                for (int row = 0; row < model.getRowCount(); row++) {
                    Boolean isSelected = (Boolean) model.getValueAt(row, 0);
                    if (Boolean.TRUE.equals(isSelected)) {
                        selectedCases.add(allCasesInSheet.get(row));
                    }
                }
            }
        }
        return selectedCases;
    }
}