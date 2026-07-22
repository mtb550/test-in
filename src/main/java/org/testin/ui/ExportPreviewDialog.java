package org.testin.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.FileTypes;
import org.testin.pojo.Group;
import org.testin.pojo.Priority;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExportPreviewDialog extends DialogWrapper {

    private static final FileTypes[] FORMATS = FileTypes.values();
    private final Project project;

    private final List<TestEditorAttributes> importAttributes = Arrays.stream(TestEditorAttributes.values())
            .filter(TestEditorAttributes::isImportable)
            .toList();

    private final TextFieldWithBrowseButton folderField;
    private final JTextField fileNameField;
    private final JComboBox<String> formatCombo;
    private final Map<String, List<TestCaseDto>> originalSheetsData;
    @Getter
    private FileTypes selectedFormat;
    @Getter
    private File selectedFile;

    public ExportPreviewDialog(final @Nullable Project project, final Map<String, List<TestCaseDto>> sheetsData, final VirtualFile exportTarget) {
        super(project, true);
        this.project = project;
        this.originalSheetsData = sheetsData;

        setTitle("Export Test Cases");
        setOKButtonText("Export");

        folderField = new TextFieldWithBrowseButton();
        fileNameField = new JTextField(30);
        formatCombo = new ComboBox<>(Arrays.stream(FORMATS).map(FileTypes::getLabel).toArray(String[]::new));

        String dirName = exportTarget.getName();
        fileNameField.setText(dirName);
        formatCombo.setSelectedItem(FileTypes.XLSX.getLabel());

        folderField.addBrowseFolderListener(
                null,
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Select Export Folder")
                        .withDescription("Choose the folder to save the export file in"),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );

        setCancelButtonText("Cancel");
        init();
        setSize(900, 600);

        triggerBrowseLater();
    }

    // todo: to be updated and remove SuppressWarnings
    @SuppressWarnings("removal")
    private void triggerBrowseLater() {
        SwingUtilities.invokeLater(() -> folderField.getButton().doClick());
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        topPanel.add(new JLabel("Destination:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        topPanel.add(folderField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        topPanel.add(new JLabel("File name:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        topPanel.add(fileNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        topPanel.add(new JLabel("Format:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        topPanel.add(formatCombo, gbc);

        panel.add(topPanel, BorderLayout.NORTH);

        JComponent tablePanel = createTablePanel();
        panel.add(tablePanel, BorderLayout.CENTER);

        return panel;
    }

    private JComponent createTablePanel() {
        JBTabbedPane tableTabbedPane = new JBTabbedPane();

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
                int maxWidth;

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
            tableTabbedPane.addTab(sheetName, scrollPane);
        }

        return tableTabbedPane;
    }

    @Override
    protected void doOKAction() {
        String folder = folderField.getText().trim();
        String fileName = fileNameField.getText().trim();
        if (fileName.isEmpty()) {
            fileNameField.requestFocus();
            return;
        }
        if (folder.isEmpty()) {
            folderField.getTextField().requestFocus();
            return;
        }
        FileTypes fmt = FileTypes.valueOf((String) formatCombo.getSelectedItem());

        String ext = fmt.getExtension();
        if (!fileName.endsWith(ext)) {
            int dot = fileName.lastIndexOf('.');
            fileName = dot >= 0 ? fileName.substring(0, dot) + ext : fileName + ext;
        }
        selectedFile = new File(folder, fileName);
        selectedFormat = fmt;

        super.doOKAction();
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