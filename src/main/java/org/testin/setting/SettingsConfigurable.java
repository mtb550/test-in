package org.testin.setting;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.ExplorerPanel;
import org.testin.explorer.toolbar.RefreshAction;
import org.testin.logger.Level;
import org.testin.logger.Logger;
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

    /**
     * The account this machine uses on a test project's server (#94).
     * <p>
     * Here rather than in {@code testin.yml}, which is committed: the server
     * address is the team's and belongs in that file, but who connects is the
     * person's. The password is not here either - it goes to the IDE's
     * credential store, because this file is plain text on disk.
     */
    private final @NotNull JBTextField sftpUserField = new JBTextField();

    private final @NotNull TextFieldWithBrowseButton sftpKeyFileField = new TextFieldWithBrowseButton();
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
                .addLabeledComponent("Log level: ", logLevelComboBox)
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel("Tester name: "), testerNameField, 1, false)
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel("Tester role: "), testerRoleField, 1, false)
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel("Default download folder: "), downloadFolderField, 1, false)
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel("SFTP account: "), sftpUserField, 1, false)
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel("SFTP key file: "), sftpKeyFileField, 1, false)
                .addComponentFillVertically(new JBPanel<>(), 0)
                .getPanel();
    }

    @Override
    public boolean isModified() {
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);
        boolean modified = !testinPathPanel.getPathText().equals(settings.rootTestinPath);
        modified |= !Objects.equals(logLevelComboBox.getSelectedItem(), settings.logLevel);
        modified |= !testerNameField.getText().equals(settings.testerName);
        modified |= !testerRoleField.getText().equals(settings.testerRole);
        modified |= !downloadFolderField.getText().equals(settings.defaultDownloadFolder);
        modified |= !sftpUserField.getText().equals(settings.sftpUser);
        modified |= !sftpKeyFileField.getText().equals(settings.sftpKeyFile);
        return modified;
    }

    @Override
    public void apply() {
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);

        // Decided before the fields are overwritten: a moved root is the only change
        // that invalidates the tree, and re-indexing is far too heavy to run for a
        // renamed tester.
        final boolean rootChanged = TestinRoot.isRootChanged(settings.rootTestinPath, testinPathPanel.getPathText());

        settings.rootTestinPath = testinPathPanel.getPathText();
        settings.logLevel = Objects.requireNonNullElse((String) logLevelComboBox.getSelectedItem(),
                Level.INFO.name());
        settings.testerName = testerNameField.getText();
        settings.testerRole = testerRoleField.getText();
        settings.defaultDownloadFolder = downloadFolderField.getText();
        settings.sftpUser = sftpUserField.getText().trim();
        settings.sftpKeyFile = sftpKeyFileField.getText().trim();

        Logger.setLogLevel(Level.valueOf(settings.logLevel));

        if (rootChanged) refreshEveryOpenProject();
    }

    /**
     * Every open project, not the one whose settings happened to be open: the
     * page is application-level now, and the root it just changed is the root all
     * of them build their tree from. Leaving the others on the previous root left
     * them showing a tree for a directory the tester had moved away from (#70).
     * <p>
     * Every project that has a panel, that is. A project that never opened the
     * Testin tool window has nothing on screen to correct, and asking its service
     * container for one would build the panel and start indexing there - a
     * refresh of something the tester never opened (#77).
     */
    private void refreshEveryOpenProject() {
        for (final Project open : ProjectManager.getInstance().getOpenProjects()) {
            if (open.isDisposed() || !Services.isCreated(open, ExplorerPanel.class)) continue;

            new RefreshAction(open, Services.getInstance(open, ExplorerPanel.class)).execute();
        }
    }

    @Override
    public void reset() {
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);

        testinPathPanel.setPathText(settings.rootTestinPath);
        logLevelComboBox.setSelectedItem(settings.logLevel);
        testerNameField.setText(settings.testerName);
        testerRoleField.setText(settings.testerRole);
        downloadFolderField.setText(settings.defaultDownloadFolder);
        sftpUserField.setText(settings.sftpUser);
        sftpKeyFileField.setText(settings.sftpKeyFile);
    }

}
