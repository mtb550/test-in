package org.testin.generateReport;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.ui.components.JBCheckBox;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.FileTypes;
import org.testin.settings.AppSettingsState;
import org.testin.util.services.Services;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;

public class GenerateReportDialog extends DialogWrapper {

    private final @NotNull Project p;

    private final TextFieldWithBrowseButton folderField = new TextFieldWithBrowseButton();

    private final JTextField fileNameField = new JTextField(30);

    private final ComboBox<String> formatCombo = new ComboBox<>(Arrays.stream(FileTypes.values())
            .filter(type -> type.getReportHandler() != null)
            .map(FileTypes::getLabel)
            .toArray(String[]::new));
    private final JBCheckBox setDefaultCheckBox = new JBCheckBox("Set as default folder");
    @Getter
    private FileTypes selectedFormat;
    @Getter
    private File selectedFile;

    public GenerateReportDialog(final @NotNull Project p, final String suggestedFileName) {
        super(p, true);
        this.p = p;

        setTitle("Generate Report");
        setOKButtonText("Generate");

        fileNameField.setText(suggestedFileName);
        formatCombo.setSelectedItem("PDF");

        FileChooserDescriptor descriptor = FileChooserDescriptorFactory
                .createSingleFolderDescriptor()
                .withTitle("Select Destination Folder")
                .withDescription("Choose the folder to save the report in");

        folderField.addBrowseFolderListener(p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        init();
        setSize(450, 200);

        String defaultFolder = Services.getInstance(p, AppSettingsState.class).defaultDownloadFolder;
        if (defaultFolder != null && !defaultFolder.trim().isEmpty()) {
            folderField.setText(defaultFolder);
        } else {
            ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> browseListener = new ComponentWithBrowseButton.BrowseFolderActionListener<>(
                    folderField, p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
            folderField.addActionListener(browseListener);
            SwingUtilities.invokeLater(() -> browseListener.actionPerformed(new ActionEvent(folderField.getTextField(), ActionEvent.ACTION_PERFORMED, "browse")));
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Destination:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(folderField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("File name:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(fileNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        panel.add(new JLabel("Format:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(formatCombo, gbc);

        String defaultFolder = Services.getInstance(p, AppSettingsState.class).defaultDownloadFolder;
        if (defaultFolder == null || defaultFolder.trim().isEmpty()) {
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.anchor = GridBagConstraints.WEST;
            panel.add(new JPanel(), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            panel.add(setDefaultCheckBox, gbc);
        }

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

        String selectedLabel = (String) formatCombo.getSelectedItem();
        FileTypes fmt = FileTypes.valueOf(selectedLabel);

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
