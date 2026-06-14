package org.testin.settings;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.Refresh;
import org.testin.projectPanel.ProjectPanel;
import org.testin.settings.ui.ProjectManagementPanel;
import org.testin.settings.ui.TestinPathPanel;
import org.testin.util.Bundle;
import org.testin.util.Tools;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

@Service(Service.Level.APP)
public final class SettingsConfigurable implements Configurable {

    private final TestinPathPanel testinPathPanel = new TestinPathPanel();
    private final JBTextField rootAutomationPathField = new JBTextField();
    private final JBCheckBox readModeCheckBox = new JBCheckBox("Enable read mode (view only)");
    private final ComboBox<String> logLevelComboBox;
    private final ProjectManagementPanel projectPanel;

    public SettingsConfigurable() {
        this.logLevelComboBox = new ComboBox<>(
                Arrays.stream(Log.Level.values()).map(Log.Level::name).toArray(String[]::new));
        this.projectPanel = new ProjectManagementPanel(testinPathPanel);
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

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Root testin folder: "), testinPathPanel.getComponent(), 1, false)
                .addLabeledComponent(new JBLabel("Root Automation folder: "), rootAutomationPathField, 1, false)
                .addVerticalGap(10)
                .addComponent(new TitledSeparator("Project Management"))
                .addLabeledComponent("Select test project: ", projectPanel.getProjectComboBox())
                .addComponent(projectPanel.getButtonPanel())
                .addVerticalGap(5)
                .addComponent(readModeCheckBox)
                .addVerticalGap(5)
                .addLabeledComponent("Log level: ", logLevelComboBox)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    @Override
    public boolean isModified() {
        AppSettingsState settings = AppSettingsState.getInstance();
        boolean modified = !testinPathPanel.getPathText().equals(settings.rootTestinPath);
        modified |= !rootAutomationPathField.getText().equals(settings.rootAutomationPath);
        modified |= readModeCheckBox.isSelected() != settings.readMode;
        modified |= !Objects.equals(logLevelComboBox.getSelectedItem(), settings.logLevel);
        return modified;
    }

    @Override
    public void apply() {
        AppSettingsState settings = AppSettingsState.getInstance();

        settings.rootTestinPath = testinPathPanel.getPathText();
        settings.rootAutomationPath = rootAutomationPathField.getText();
        settings.readMode = readModeCheckBox.isSelected();
        settings.logLevel = (String) logLevelComboBox.getSelectedItem();

        Log.setLogLevel(Log.Level.valueOf(settings.logLevel));

        Project project = getProject();
        Setting setting = Services.getInstance(project, Setting.class);

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

        projectPanel.refreshProjectList();

        ProjectPanel panel = Services.getInstance(project, ProjectPanel.class);
        if (panel != null) {
            new Refresh(panel).execute();
        }
    }

    @Override
    public void reset() {
        AppSettingsState settings = AppSettingsState.getInstance();

        testinPathPanel.setPathText(settings.rootTestinPath);

        Project project = getProject();
        VirtualFile mainSourceRoot = Services.getInstance(project, Tools.class)
                .getTestSourceRoot(project);
        if (mainSourceRoot != null) {
            rootAutomationPathField.setText(mainSourceRoot.getPath());
        } else {
            rootAutomationPathField.setText(settings.rootAutomationPath != null
                    ? settings.rootAutomationPath : "No source root detected");
        }

        readModeCheckBox.setSelected(settings.readMode);
        logLevelComboBox.setSelectedItem(settings.logLevel);
        projectPanel.refreshProjectList();
    }

    private Project getProject() {

        return ProjectManager.getInstance().getDefaultProject();
    }
}
