package org.testin.setting;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Level;
import org.testin.logger.Logger;
import org.testin.explorer.ProjectPanel;
import org.testin.explorer.toolBar.RefreshAction;
import org.testin.services.Services;
import org.testin.setting.dialogs.TestinPathPanel;
import org.testin.util.Bundle;

import javax.swing.*;
import java.util.Arrays;
import java.util.Objects;

public final class SettingsConfigurable implements Configurable {

    private final @NotNull TestinPathPanel testinPathPanel;
    private final @NotNull JBTextField testerNameField = new JBTextField();
    private final @NotNull JBTextField testerRoleField = new JBTextField();
    private final @NotNull TextFieldWithBrowseButton downloadFolderField = new TextFieldWithBrowseButton();
    private final @NotNull JBCheckBox openTreeOnStartupCheckBox = new JBCheckBox("Open the Testin panel when a project opens");
    private final @NotNull ComboBox<String> logLevelComboBox;

    public SettingsConfigurable() {
        testinPathPanel = new TestinPathPanel();
        this.logLevelComboBox = new ComboBox<>(Arrays.stream(Level.values()).map(Level::name).toArray(String[]::new));
    }

    @Override
    public @NotNull String getDisplayName() {
        return Bundle.getPluginName();
    }

    @Override
    public @NotNull JComponent createComponent() {
        // Null project, as the source-root field above already does: the chooser
        // needs one only to seed a starting directory, and an application-level
        // page has none to give it (#70).
        downloadFolderField.addBrowseFolderListener(null, FileChooserDescriptorFactory.createSingleFolderDescriptor()
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
        final AppSettingsState settings = Services.getInstance(AppSettingsState.class);
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
        final AppSettingsState settings = Services.getInstance(AppSettingsState.class);

        // Decided before the fields are overwritten: a moved root is the only change
        // that invalidates the tree, and re-indexing is far too heavy to run for a
        // renamed tester.
        final boolean rootChanged = Setting.isRootChanged(settings.rootTestinPath, testinPathPanel.getPathText());

        settings.rootTestinPath = testinPathPanel.getPathText();
        settings.openTreeOnStartup = openTreeOnStartupCheckBox.isSelected();
        final String selectedLogLevel = (String) logLevelComboBox.getSelectedItem();
        settings.logLevel = selectedLogLevel != null ? selectedLogLevel : Level.INFO.name();
        settings.testerName = testerNameField.getText();
        settings.testerRole = testerRoleField.getText();
        settings.defaultDownloadFolder = downloadFolderField.getText();

        Logger.setLogLevel(Level.valueOf(settings.logLevel));

        if (rootChanged) refreshEveryOpenProject();
    }

    /**
     * Every open project, not the one whose settings happened to be open: the
     * page is application-level now, and the root it just changed is the root all
     * of them build their tree from. Leaving the others on the previous root left
     * them showing a tree for a directory the tester had moved away from (#70).
     */
    private void refreshEveryOpenProject() {
        for (final Project open : ProjectManager.getInstance().getOpenProjects()) {
            if (open.isDisposed()) continue;

            new RefreshAction(open, Services.getInstance(open, ProjectPanel.class)).execute();
        }
    }

    @Override
    public void reset() {
        final AppSettingsState settings = Services.getInstance(AppSettingsState.class);

        testinPathPanel.setPathText(settings.rootTestinPath);
        openTreeOnStartupCheckBox.setSelected(settings.openTreeOnStartup);
        logLevelComboBox.setSelectedItem(settings.logLevel);
        testerNameField.setText(settings.testerName);
        testerRoleField.setText(settings.testerRole);
        downloadFolderField.setText(settings.defaultDownloadFolder);
    }

}
