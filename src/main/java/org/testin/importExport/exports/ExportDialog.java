package org.testin.importExport.exports;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.FileTypes;
import org.testin.enums.TestEditorAttributes;
import org.testin.importExport.shared.TablePanelBuilder;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.settings.AppSettingsState;
import org.testin.ui.dialogs.FramelessDialogWrapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExportDialog extends FramelessDialogWrapper {

    private final @NotNull Project p;

    private final @NotNull List<TestEditorAttributes> exportAttributes;

    private final @NotNull TextFieldWithBrowseButton folderField = new TextFieldWithBrowseButton();

    private final @NotNull JBTextField fileNameField = new JBTextField(30);

    // Offer only formats that actually have an export handler (PDF/Word are report-only).
    private final @NotNull ComboBox<String> formatCombo = new ComboBox<>(Arrays.stream(FileTypes.values())
            .filter(type -> type.getExportHandler() != null)
            .map(FileTypes::getLabel)
            .toArray(String[]::new));

    private final @NotNull Map<String, List<TestCaseDto>> originalSheetsData;

    private final @NotNull JBCheckBox setDefaultCheckBox = new JBCheckBox("Set as default folder");

    /**
     * Both null until the dialog is accepted; read by the caller only after showAndGet.
     */
    @Getter
    private @Nullable FileTypes selectedFormat;

    @Getter
    private @Nullable File selectedFile;

    public ExportDialog(final @NotNull Project p, final @NotNull List<TestEditorAttributes> exportAttributes,
                        final @NotNull Map<String, List<TestCaseDto>> sheetsData,
                        final @NotNull VirtualFile exportTarget) {
        super(p, true);
        this.p = p;
        this.exportAttributes = exportAttributes;
        this.originalSheetsData = sheetsData;

        setTitle("Export Test Cases");

        fileNameField.setText(exportTarget.getName());
        formatCombo.setSelectedItem(FileTypes.XLSX.getLabel());

        final FileChooserDescriptor descriptor = FileChooserDescriptorFactory
                .createSingleFolderDescriptor()
                .withTitle("Select Export Folder")
                .withDescription("Choose the folder to save the export file in");

        folderField.addBrowseFolderListener(p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        initFrameless();
        setSize(900, 600);

        final String defaultFolder = Services.getInstance(p, AppSettingsState.class).defaultDownloadFolder;
        if (!defaultFolder.isBlank()) {
            folderField.setText(defaultFolder);
        } else {
            final ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> browseListener = new ComponentWithBrowseButton.BrowseFolderActionListener<>(folderField, p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
            folderField.addActionListener(browseListener);
            ApplicationManager.getApplication().invokeLater(() -> browseListener.actionPerformed(new ActionEvent(folderField.getTextField(), ActionEvent.ACTION_PERFORMED, "browse")));
        }
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
        topPanel.add(new JBLabel("Destination:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        topPanel.add(folderField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        topPanel.add(new JBLabel("File name:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        topPanel.add(fileNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        topPanel.add(new JBLabel("Format:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        topPanel.add(formatCombo, gbc);

        final String defaultFolder = Services.getInstance(p, AppSettingsState.class).defaultDownloadFolder;
        if (defaultFolder.isBlank()) {
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.anchor = GridBagConstraints.WEST;
            topPanel.add(new JBPanel<>(), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            topPanel.add(setDefaultCheckBox, gbc);
        }

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new TablePanelBuilder().createTabbedPane(originalSheetsData, exportAttributes, p, model -> {
        }), BorderLayout.CENTER);

        return panel;
    }

    @Override
    protected void doOKAction() {
        final String folder = folderField.getText().trim();
        String fileName = fileNameField.getText().trim();
        if (fileName.isEmpty()) {
            fileNameField.requestFocus();
            return;
        }
        if (folder.isEmpty()) {
            folderField.getTextField().requestFocus();
            return;
        }

        final String selectedLabel = (String) formatCombo.getSelectedItem();
        final FileTypes fmt = selectedLabel == null ? null : FileTypes.fromLabel(selectedLabel);
        if (fmt == null) {
            formatCombo.requestFocus();
            return;
        }

        final String ext = fmt.getExtension();
        if (!fileName.endsWith(ext)) {
            final int dot = fileName.lastIndexOf('.');
            fileName = dot >= 0 ? fileName.substring(0, dot) + ext : fileName + ext;
        }

        selectedFile = new File(folder, fileName);
        selectedFormat = fmt;

        if (setDefaultCheckBox.isSelected()) {
            Services.getInstance(p, AppSettingsState.class).defaultDownloadFolder = folder;
        }

        super.doOKAction();
    }
}
