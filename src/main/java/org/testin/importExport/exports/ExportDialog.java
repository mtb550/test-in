package org.testin.importExport.exports;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.FileTypes;
import org.testin.enums.TestEditorAttributes;
import org.testin.importExport.shared.TablePanelBuilder;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExportDialog extends DialogWrapper {

    private final @NotNull Project project;

    private final List<TestEditorAttributes> exportAttributes;

    private final TextFieldWithBrowseButton folderField = new TextFieldWithBrowseButton();

    private final JTextField fileNameField = new JTextField(30);

    private final JComboBox<String> formatCombo = new ComboBox<>(Arrays.stream(FileTypes.values()).map(FileTypes::getLabel).toArray(String[]::new));

    private final Map<String, List<TestCaseDto>> originalSheetsData;

    @Getter
    private FileTypes selectedFormat;

    @Getter
    private File selectedFile;

    public ExportDialog(final @NotNull Project project, final List<TestEditorAttributes> exportAttributes, final Map<String, List<TestCaseDto>> sheetsData, final VirtualFile exportTarget) {
        super(project, true);
        this.project = project;
        this.exportAttributes = exportAttributes;
        this.originalSheetsData = sheetsData;

        setTitle("Export Test Cases");
        setOKButtonText("Export");

        String dirName = exportTarget.getName();
        fileNameField.setText(dirName);
        formatCombo.setSelectedItem(FileTypes.XLSX.getLabel());

        FileChooserDescriptor descriptor = FileChooserDescriptorFactory
                .createSingleFolderDescriptor()
                .withTitle("Select Export Folder")
                .withDescription("Choose the folder to save the export file in");

        folderField.addBrowseFolderListener(project, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        setCancelButtonText("Cancel");
        init();
        setSize(900, 600);

        ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> browseListener = new ComponentWithBrowseButton.BrowseFolderActionListener<>(folderField, project, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        folderField.addActionListener(browseListener);
        SwingUtilities.invokeLater(() -> browseListener.actionPerformed(new ActionEvent(folderField.getTextField(), ActionEvent.ACTION_PERFORMED, "browse")));
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
        panel.add(new TablePanelBuilder().createTabbedPane(originalSheetsData, exportAttributes, project, model -> {
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

        super.doOKAction();
    }
}
