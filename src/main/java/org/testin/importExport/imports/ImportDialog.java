package org.testin.importExport.imports;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.*;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.FileTypes;
import org.testin.enums.TestEditorAttributes;
import org.testin.importExport.shared.CellEditListener;
import org.testin.importExport.shared.FileDocumentListener;
import org.testin.importExport.shared.TablePanelBuilder;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.settings.AppSettingsState;
import org.testin.ui.dialogs.FramelessDialogWrapper;

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

public class ImportDialog extends FramelessDialogWrapper {
    private final @NotNull Map<String, DefaultTableModel> tableModelsMap = new LinkedHashMap<>();

    private final @NotNull Project p;

    private final @NotNull List<TestEditorAttributes> importAttributes;

    private final @NotNull TextFieldWithBrowseButton fileField = new TextFieldWithBrowseButton();

    private final @NotNull JBCheckBox setDefaultCheckBox = new JBCheckBox("Set as default folder");
    // Created here rather than in createCenterPanel: the file listener can call
    // onDataLoaded as soon as the dialog is constructed.
    private final @NotNull JBTabbedPane tableTabbedPane = new JBTabbedPane();
    private @NotNull Map<String, List<TestCaseDto>> originalSheetsData = new LinkedHashMap<>();

    public ImportDialog(final @NotNull Project p, final @NotNull List<TestEditorAttributes> importAttributes,
                        final @NotNull BiFunction<File, FileTypes, Map<String, List<TestCaseDto>>> importLoader) {
        super(p, true);
        this.p = p;
        this.importAttributes = importAttributes;

        setTitle("Import Test Cases");

        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withExtensionFilter("", "xls", "xlsx", "csv", "json")
                .withTitle("Select Import File")
                .withDescription("Choose a file to import test cases from (.xls, .xlsx, .json, .csv)");

        fileField.addBrowseFolderListener(p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        final FileDocumentListener fileListener = new FileDocumentListener(fileField, p, this::onDataLoaded, importLoader);
        fileField.getTextField().getDocument().addDocumentListener(fileListener);

        initFrameless();
        setSize(900, 600);

        final String defaultFolder = Services.getInstance(p, AppSettingsState.class).defaultDownloadFolder;
        if (defaultFolder != null && !defaultFolder.trim().isEmpty()) {
            fileField.setText(defaultFolder);
        } else {
            final ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> browseListener = new ComponentWithBrowseButton.BrowseFolderActionListener<>(fileField, p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
            fileField.addActionListener(browseListener);
            ApplicationManager.getApplication().invokeLater(() -> browseListener.actionPerformed(new ActionEvent(fileField.getTextField(), ActionEvent.ACTION_PERFORMED, "browse")));
        }
    }

    private void onDataLoaded(final @NotNull Map<String, List<TestCaseDto>> parsedData) {
        this.originalSheetsData = parsedData;

        while (tableTabbedPane.getTabCount() > 0) {
            tableTabbedPane.removeTabAt(0);
        }
        populateTabs();
    }

    @Override
    protected @NotNull JComponent createCenterPanel() {
        final JBPanel<?> panel = new JBPanel<>(new BorderLayout());

        final JBPanel<?> topPanel = new JBPanel<>(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        topPanel.add(new JBLabel("Source:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        topPanel.add(fileField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        topPanel.add(new JBLabel("Options:"), gbc);

        final String defaultFolder = Services.getInstance(p, AppSettingsState.class).defaultDownloadFolder;
        if (defaultFolder == null || defaultFolder.trim().isEmpty()) {
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.anchor = GridBagConstraints.WEST;
            topPanel.add(new JBPanel<>(), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            topPanel.add(setDefaultCheckBox, gbc);
        }

        panel.add(topPanel, BorderLayout.NORTH);

        populateTabs();
        panel.add(tableTabbedPane, BorderLayout.CENTER);

        return panel;
    }

    private void populateTabs() {
        for (final Map.Entry<String, List<TestCaseDto>> entry : originalSheetsData.entrySet()) {
            final String sheetName = entry.getKey();
            final List<TestCaseDto> testCases = entry.getValue();

            final DefaultTableModel model = new TablePanelBuilder().createModel(p, importAttributes, testCases);
            model.addTableModelListener(new CellEditListener(importAttributes, p, testCases));
            tableModelsMap.put(sheetName, model);
            tableTabbedPane.addTab(sheetName, new JBScrollPane(new TablePanelBuilder().buildTable(model, p)));
        }
    }

    @Override
    protected void doOKAction() {
        final String filePath = fileField.getText().trim();
        if (filePath.isEmpty()) {
            Services.getInstance(p, Notifier.class).error(p, "Import Error", "Please select a source file first.");
            return;
        }

        if (originalSheetsData.isEmpty()) {
            Services.getInstance(p, Notifier.class).error(p, "Import Error", "No data loaded from the selected file.");
            return;
        }

        boolean hasSelection = false;
        for (final Map.Entry<String, DefaultTableModel> entry : tableModelsMap.entrySet()) {
            final DefaultTableModel model = entry.getValue();
            for (int row = 0; row < model.getRowCount(); row++) {
                if (Boolean.TRUE.equals(model.getValueAt(row, 0))) {
                    hasSelection = true;
                    break;
                }
            }
            if (hasSelection) break;
        }

        if (!hasSelection) {
            Services.getInstance(p, Notifier.class).error(p, "Import Error", "Please select at least one test case to import.");
            return;
        }

        if (setDefaultCheckBox.isSelected()) {
            final File parentFolder = new File(filePath).getParentFile();
            if (parentFolder != null) {
                Services.getInstance(p, AppSettingsState.class).defaultDownloadFolder = parentFolder.getAbsolutePath();
            }
        }

        super.doOKAction();
    }

    public @NotNull Map<String, List<TestCaseDto>> getSelectedTestCasesBySheet() {
        final Map<String, List<TestCaseDto>> selectedCasesBySheet = new LinkedHashMap<>();

        for (final Map.Entry<String, List<TestCaseDto>> entry : originalSheetsData.entrySet()) {
            final String sheetName = entry.getKey();
            final List<TestCaseDto> allCasesInSheet = entry.getValue();
            final DefaultTableModel model = tableModelsMap.get(sheetName);

            final List<TestCaseDto> selectedCases = new ArrayList<>();
            if (model != null) {
                for (int row = 0; row < model.getRowCount(); row++) {
                    if (Boolean.TRUE.equals(model.getValueAt(row, 0))) {
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
