package org.testin.generateReport;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.FileTypes;
import org.testin.services.Services;
import org.testin.settings.AppSettingsState;
import org.testin.ui.dialogs.FramelessDialogWrapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;

public class GenerateReportDialog extends FramelessDialogWrapper {

    private final @NotNull Project p;

    private final @NotNull TextFieldWithBrowseButton folderField = new TextFieldWithBrowseButton();

    private final @NotNull JBTextField fileNameField = new JBTextField(30);

    private final @NotNull ComboBox<FileTypes> formatCombo = new ComboBox<>(Arrays.stream(FileTypes.values())
            .filter(FileTypes::isReportable)
            .toArray(FileTypes[]::new));
    private final @NotNull JBCheckBox setDefaultCheckBox = new JBCheckBox("Set as default folder");

    /**
     * Both null until the dialog is accepted; read by the caller only after showAndGet.
     */
    @Getter
    private @Nullable FileTypes selectedFormat;
    @Getter
    private @Nullable File selectedFile;

    public GenerateReportDialog(final @NotNull Project p, final @NotNull String suggestedFileName) {
        super(p, true);
        this.p = p;

        setTitle("Generate Report");

        fileNameField.setText(suggestedFileName);
        formatCombo.setSelectedItem(FileTypes.PDF);
        // The combo holds the format itself and renders its label, so the
        // selection needs no lookup back from text.
        formatCombo.setRenderer(SimpleListCellRenderer.create("", FileTypes::getLabel));


        final FileChooserDescriptor descriptor = FileChooserDescriptorFactory
                .createSingleFolderDescriptor()
                .withTitle("Select Destination Folder")
                .withDescription("Choose the folder to save the report in");

        folderField.addBrowseFolderListener(p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        initFrameless();
        setSize(450, 200);

        final String defaultFolder = Services.getInstance(p, AppSettingsState.class).defaultDownloadFolder;
        if (!defaultFolder.isBlank()) {
            folderField.setText(defaultFolder);
        } else {
            // Fired directly, not registered: addBrowseFolderListener above already
            // owns the button, and registering this one too opened the chooser a
            // second time as soon as the first closed.
            final ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> browseListener = new ComponentWithBrowseButton.BrowseFolderActionListener<>(
                    folderField, p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
            ApplicationManager.getApplication().invokeLater(() -> browseListener.actionPerformed(new ActionEvent(folderField.getTextField(), ActionEvent.ACTION_PERFORMED, "browse")));
        }
    }

    @Override
    protected @NotNull JComponent createCenterPanel() {
        final JBPanel<?> panel = new JBPanel<>(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JBLabel("Destination:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(folderField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JBLabel("File name:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(fileNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        panel.add(new JBLabel("Format:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(formatCombo, gbc);

        final String defaultFolder = Services.getInstance(p, AppSettingsState.class).defaultDownloadFolder;
        if (defaultFolder.isBlank()) {
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.anchor = GridBagConstraints.WEST;
            panel.add(new JBPanel<>(), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            panel.add(setDefaultCheckBox, gbc);
        }

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

        final FileTypes fmt = (FileTypes) formatCombo.getSelectedItem();
        if (fmt == null) {
            formatCombo.requestFocus();
            return;
        }

        final String ext = fmt.getExtension();
        if (!fileName.endsWith(ext)) {
            // Only a tail that is itself a known extension is replaced. Cutting at
            // the last dot regardless turned "Sprint 1.2 Report" into "Sprint 1.pdf".
            final int dot = fileName.lastIndexOf('.');
            final String tail = dot >= 0 ? fileName.substring(dot) : "";
            final boolean tailIsAnExtension = Arrays.stream(FileTypes.values())
                    .anyMatch(type -> type.getExtension().equalsIgnoreCase(tail));

            fileName = tailIsAnExtension ? fileName.substring(0, dot) + ext : fileName + ext;
        }

        selectedFile = new File(folder, fileName);
        selectedFormat = fmt;

        if (setDefaultCheckBox.isSelected()) {
            Services.getInstance(p, AppSettingsState.class).defaultDownloadFolder = folder;
        }

        super.doOKAction();
    }
}
