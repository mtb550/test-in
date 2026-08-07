package org.testin.importExport.imports;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTabbedPane;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.FileTypes;
import org.testin.enums.TestEditorAttributes;
import org.testin.generateJavaCode.CodeGeneratorDialog;
import org.testin.generateJavaCode.GeneratorType;
import org.testin.importExport.shared.CellEditListener;
import org.testin.importExport.shared.FileDocumentListener;
import org.testin.importExport.shared.TablePanelBuilder;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.settings.AppSettingsState;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class ImportDialog extends DialogWrapper {
    private final Map<String, DefaultTableModel> tableModelsMap = new LinkedHashMap<>();

    private final Project project;

    @Getter
    private final CodeGeneratorDialog cg = new CodeGeneratorDialog(GeneratorType.CREATE_TEST_CASE);

    private final List<TestEditorAttributes> importAttributes;

    private final TextFieldWithBrowseButton fileField = new TextFieldWithBrowseButton();

    private final JBCheckBox setDefaultCheckBox = new JBCheckBox("Set as default folder");

    private Map<String, List<TestCaseDto>> originalSheetsData = new LinkedHashMap<>();

    private JBTabbedPane tableTabbedPane;

    public ImportDialog(final @NotNull Project p, final @NotNull List<TestEditorAttributes> importAttributes, final @NotNull BiFunction<File, FileTypes, Map<String, List<TestCaseDto>>> importLoader) {
        super(p, true);
        this.project = p;
        this.importAttributes = importAttributes;

        cg.setText("create test methods");
        setTitle("Import Test Cases");
        setOKButtonText("Import Selected");

        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withExtensionFilter("", "xls", "xlsx", "csv", "json")
                .withTitle("Select Import File")
                .withDescription("Choose a file to import test cases from (.xls, .xlsx, .json, .csv)");

        fileField.addBrowseFolderListener(p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        FileDocumentListener fileListener = new FileDocumentListener(fileField, p, this::onDataLoaded, importLoader);
        fileField.getTextField().getDocument().addDocumentListener(fileListener);

        setCancelButtonText("Cancel");
        init();
        setSize(900, 600);

        String defaultFolder = AppSettingsState.getInstance().defaultDownloadFolder;
        if (defaultFolder != null && !defaultFolder.trim().isEmpty()) {
            fileField.setText(defaultFolder);
        } else {
            ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> browseListener = new ComponentWithBrowseButton.BrowseFolderActionListener<>(fileField, p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
            fileField.addActionListener(browseListener);
            SwingUtilities.invokeLater(() -> browseListener.actionPerformed(new ActionEvent(fileField.getTextField(), ActionEvent.ACTION_PERFORMED, "browse")));
        }
    }

    private void onDataLoaded(final Map<String, List<TestCaseDto>> parsedData) {
        this.originalSheetsData = parsedData;

        while (tableTabbedPane.getTabCount() > 0) {
            tableTabbedPane.removeTabAt(0);
        }
        populateTabs();
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
        topPanel.add(new JLabel("Source:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        topPanel.add(fileField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        topPanel.add(new JLabel("Options:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        topPanel.add(cg, gbc);

        String defaultFolder = AppSettingsState.getInstance().defaultDownloadFolder;
        if (defaultFolder == null || defaultFolder.trim().isEmpty()) {
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.anchor = GridBagConstraints.WEST;
            topPanel.add(new JPanel(), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            topPanel.add(setDefaultCheckBox, gbc);
        }

        panel.add(topPanel, BorderLayout.NORTH);

        tableTabbedPane = new JBTabbedPane();
        populateTabs();
        panel.add(tableTabbedPane, BorderLayout.CENTER);

        return panel;
    }

    private void populateTabs() {
        for (Map.Entry<String, List<TestCaseDto>> entry : originalSheetsData.entrySet()) {
            String sheetName = entry.getKey();
            List<TestCaseDto> testCases = entry.getValue();

            DefaultTableModel model = new TablePanelBuilder().createModel(project, importAttributes, testCases);
            model.addTableModelListener(new CellEditListener(importAttributes, project, testCases));
            tableModelsMap.put(sheetName, model);
            tableTabbedPane.addTab(sheetName, new JScrollPane(new TablePanelBuilder().buildTable(model, project)));
        }
    }

    @Override
    protected void doOKAction() {
        String filePath = fileField.getText().trim();
        if (filePath.isEmpty()) {
            Services.getInstance(project, Notifier.class).error(project, "Import Error", "Please select a source file first.");
            return;
        }

        if (originalSheetsData.isEmpty()) {
            Services.getInstance(project, Notifier.class).error(project, "Import Error", "No data loaded from the selected file.");
            return;
        }

        boolean hasSelection = false;
        for (Map.Entry<String, DefaultTableModel> entry : tableModelsMap.entrySet()) {
            DefaultTableModel model = entry.getValue();
            for (int row = 0; row < model.getRowCount(); row++) {
                Boolean isSelected = (Boolean) model.getValueAt(row, 0);
                if (Boolean.TRUE.equals(isSelected)) {
                    hasSelection = true;
                    break;
                }
            }
            if (hasSelection) break;
        }

        if (!hasSelection) {
            Services.getInstance(project, Notifier.class).error(project, "Import Error", "Please select at least one test case to import.");
            return;
        }

        if (setDefaultCheckBox.isSelected()) {
            File selectedFile = new File(filePath);
            File parentFolder = selectedFile.getParentFile();
            if (parentFolder != null) {
                AppSettingsState.getInstance().defaultDownloadFolder = parentFolder.getAbsolutePath();
            }
        }

        super.doOKAction();
    }

    public Map<String, List<TestCaseDto>> getSelectedTestCasesBySheet() {
        final Map<String, List<TestCaseDto>> selectedCasesBySheet = new LinkedHashMap<>();

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
}
