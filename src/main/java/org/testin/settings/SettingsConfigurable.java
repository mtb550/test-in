package org.testin.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Level;
import org.testin.logger.Logger;
import org.testin.projectPanel.ProjectPanel;
import org.testin.projectPanel.toolBar.RefreshAction;
import org.testin.services.Services;
import org.testin.settings.Dialogs.TestinPathPanel;
import org.testin.util.Bundle;
import org.testin.util.Tools;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

public final class SettingsConfigurable implements Configurable {

    private final TestinPathPanel testinPathPanel;
    private final JBTextField rootAutomationPathField = new JBTextField();
    private final JBTextField testerNameField = new JBTextField();
    private final JBTextField testerRoleField = new JBTextField();
    private final TextFieldWithBrowseButton downloadFolderField = new TextFieldWithBrowseButton();
    private final JBCheckBox readModeCheckBox = new JBCheckBox("Enable read mode (view only)");
    private final ComboBox<String> logLevelComboBox;
    private final @NotNull Project p;

    public SettingsConfigurable(final @NotNull Project p) {
        this.p = p;
        testinPathPanel = new TestinPathPanel(p);
        this.logLevelComboBox = new ComboBox<>(Arrays.stream(Level.values()).map(Level::name).toArray(String[]::new));
    }

    @Override
    public String getDisplayName() {
        return Bundle.getPluginName();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        rootAutomationPathField.setEnabled(false);
        rootAutomationPathField.setToolTipText(
                "Automatically detected base package path for your automation framework");

        downloadFolderField.addBrowseFolderListener(p, FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Select Default Download Folder")
                        .withDescription("Choose the default folder for imports, exports, and reports"),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Testin source root: "), testinPathPanel.getComponent(), 1, false)
                .addLabeledComponent(new JBLabel("Java test source root"), rootAutomationPathField, 1, false)
                .addVerticalGap(5)
                .addComponent(readModeCheckBox)
                .addVerticalGap(5)
                .addLabeledComponent("Log level: ", logLevelComboBox)
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel("Tester name: "), testerNameField, 1, false)
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel("Tester role: "), testerRoleField, 1, false)
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel("Default download folder: "), downloadFolderField, 1, false)
                .addComponentFillVertically(new JBPanel<>(), 0)
                .getPanel();
    }

    @Override
    public boolean isModified() {
        AppSettingsState settings = Services.getInstance(p, AppSettingsState.class);
        boolean modified = !testinPathPanel.getPathText().equals(settings.rootTestinPath);
        modified |= !rootAutomationPathField.getText().equals(settings.rootAutomationPath);
        modified |= readModeCheckBox.isSelected() != settings.readMode;
        modified |= !Objects.equals(logLevelComboBox.getSelectedItem(), settings.logLevel);
        modified |= !testerNameField.getText().equals(settings.testerName);
        modified |= !testerRoleField.getText().equals(settings.testerRole);
        modified |= !downloadFolderField.getText().equals(settings.defaultDownloadFolder);
        return modified;
    }

    @Override
    public void apply() {
        AppSettingsState settings = Services.getInstance(p, AppSettingsState.class);

        settings.rootTestinPath = testinPathPanel.getPathText();
        settings.rootAutomationPath = rootAutomationPathField.getText();
        settings.readMode = readModeCheckBox.isSelected();
        settings.logLevel = (String) logLevelComboBox.getSelectedItem();
        settings.testerName = testerNameField.getText();
        settings.testerRole = testerRoleField.getText();
        settings.defaultDownloadFolder = downloadFolderField.getText();

        Logger.setLogLevel(Level.valueOf(settings.logLevel));

        Setting setting = Services.getInstance(p, Setting.class);

        if (settings.rootTestinPath != null && !settings.rootTestinPath.trim().isEmpty()) {
            setting.setTestinPath(Path.of(settings.rootTestinPath));
        } else {
            setting.setTestinPath(Path.of(""));
        }

        if (settings.rootAutomationPath != null && !settings.rootAutomationPath.trim().isEmpty()) {
            setting.setAutomationPath(Path.of(settings.rootAutomationPath));
        } else {
            setting.setAutomationPath(null);
        }

        final ProjectPanel pp = Services.getInstance(p, ProjectPanel.class);
        if (pp != null) new RefreshAction(p, pp).execute();
    }

    @Override
    public void reset() {
        AppSettingsState settings = Services.getInstance(p, AppSettingsState.class);

        testinPathPanel.setPathText(settings.rootTestinPath);

        VirtualFile mainSourceRoot = Services.getInstance(p, Tools.class)
                .getTestSourceRoot(p);
        if (mainSourceRoot != null) {
            rootAutomationPathField.setText(mainSourceRoot.getPath());
        } else {
            rootAutomationPathField.setText(settings.rootAutomationPath != null
                    ? settings.rootAutomationPath : "No source root detected");
        }

        readModeCheckBox.setSelected(settings.readMode);
        logLevelComboBox.setSelectedItem(settings.logLevel);
        testerNameField.setText(settings.testerName);
        testerRoleField.setText(settings.testerRole);
        downloadFolderField.setText(settings.defaultDownloadFolder);
    }

}
