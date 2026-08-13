package org.testin.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
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

import javax.swing.*;
import java.util.Arrays;
import java.util.Objects;

public final class SettingsConfigurable implements Configurable {

    private final TestinPathPanel testinPathPanel;
    private final JBTextField testerNameField = new JBTextField();
    private final JBTextField testerRoleField = new JBTextField();
    private final TextFieldWithBrowseButton downloadFolderField = new TextFieldWithBrowseButton();
    private final JBCheckBox openTreeOnStartupCheckBox = new JBCheckBox("Open the Testin panel when a project opens");
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
        downloadFolderField.addBrowseFolderListener(p, FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Select Default Download Folder")
                        .withDescription("Choose the default folder for imports, exports, and reports"),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Testin source root: "), testinPathPanel.getComponent(), 1, false)
                .addVerticalGap(5)
                .addComponent(openTreeOnStartupCheckBox)
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
        modified |= openTreeOnStartupCheckBox.isSelected() != settings.openTreeOnStartup;
        modified |= !Objects.equals(logLevelComboBox.getSelectedItem(), settings.logLevel);
        modified |= !testerNameField.getText().equals(settings.testerName);
        modified |= !testerRoleField.getText().equals(settings.testerRole);
        modified |= !downloadFolderField.getText().equals(settings.defaultDownloadFolder);
        return modified;
    }

    @Override
    public void apply() {
        AppSettingsState settings = Services.getInstance(p, AppSettingsState.class);

        // Decided before the fields are overwritten: a moved root is the only change
        // that invalidates the tree, and re-indexing is far too heavy to run for a
        // renamed tester.
        final boolean rootChanged = Setting.isRootChanged(settings.rootTestinPath, testinPathPanel.getPathText());

        settings.rootTestinPath = testinPathPanel.getPathText();
        settings.openTreeOnStartup = openTreeOnStartupCheckBox.isSelected();
        settings.logLevel = (String) logLevelComboBox.getSelectedItem();
        settings.testerName = testerNameField.getText();
        settings.testerRole = testerRoleField.getText();
        settings.defaultDownloadFolder = downloadFolderField.getText();

        Logger.setLogLevel(Level.valueOf(settings.logLevel));

        Setting setting = Services.getInstance(p, Setting.class);
        setting.setTestinPath(Setting.normalize(settings.rootTestinPath));

        if (rootChanged) {
            final ProjectPanel pp = Services.getInstance(p, ProjectPanel.class);
            new RefreshAction(p, pp).execute();
        }
    }

    @Override
    public void reset() {
        AppSettingsState settings = Services.getInstance(p, AppSettingsState.class);

        testinPathPanel.setPathText(settings.rootTestinPath);
        openTreeOnStartupCheckBox.setSelected(settings.openTreeOnStartup);
        logLevelComboBox.setSelectedItem(settings.logLevel);
        testerNameField.setText(settings.testerName);
        testerRoleField.setText(settings.testerRole);
        downloadFolderField.setText(settings.defaultDownloadFolder);
    }

}
