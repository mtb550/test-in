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

    private final List<TestEditorAttributes> exportAttributes;

    private final TextFieldWithBrowseButton folderField = new TextFieldWithBrowseButton();

    private final JBTextField fileNameField = new JBTextField(30);

    private final ComboBox<String> formatCombo = new ComboBox<>(Arrays.stream(FileTypes.values()).map(FileTypes::getLabel).toArray(String[]::new));

    private final Map<String, List<TestCaseDto>> originalSheetsData;

    private final JBCheckBox setDefaultCheckBox = new JBCheckBox("Set as default folder");

    @Getter
    private FileTypes selectedFormat;

    @Getter
    private File selectedFile;

    public ExportDialog(final @NotNull Project p, final List<TestEditorAttributes> exportAttributes, final Map<String, List<TestCaseDto>> sheetsData, final VirtualFile exportTarget) {
        super(p, true);
        this.p = p;
        this.exportAttributes = exportAttributes;
        this.originalSheetsData = sheetsData;

        setTitle("Export Test Cases");

        String dirName = exportTarget.getName();
        fileNameField.setText(dirName);
        formatCombo.setSelectedItem(FileTypes.XLSX.getLabel());

        FileChooserDescriptor descriptor = FileChooserDescriptorFactory
                .createSingleFolderDescriptor()
                .withTitle("Select Export Folder")
                .withDescription("Choose the folder to save the export file in");

        folderField.addBrowseFolderListener(p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        initFrameless();
        setSize(900, 600);

        String defaultFolder = Services.getInstance(p, AppSettingsState.class).defaultDownloadFolder;
        if (defaultFolder != null && !defaultFolder.trim().isEmpty()) {
            folderField.setText(defaultFolder);
        } else {
            ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> browseListener = new ComponentWithBrowseButton.BrowseFolderActionListener<>(folderField, p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
            folderField.addActionListener(browseListener);
            ApplicationManager.getApplication().invokeLater(() -> browseListener.actionPerformed(new ActionEvent(folderField.getTextField(), ActionEvent.ACTION_PERFORMED, "browse")));
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());

        JBPanel<?> topPanel = new JBPanel<>(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
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

        String defaultFolder = Services.getInstance(p, AppSettingsState.class).defaultDownloadFolder;
        if (defaultFolder == null || defaultFolder.trim().isEmpty()) {
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

        if (setDefaultCheckBox.isSelected()) {
            Services.getInstance(p, AppSettingsState.class).defaultDownloadFolder = folder;
        }

        super.doOKAction();
    }
}
