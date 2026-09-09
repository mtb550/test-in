package org.testin.setting;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
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
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.TreePanel;
import org.testin.logger.Level;
import org.testin.logger.Logger;
import org.testin.services.Services;
import org.testin.setting.dialogs.TestinPathPanel;
import org.testin.util.Bundle;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

public final class SettingsConfigurable implements SearchableConfigurable {

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

    /**
     * The strip of keys along the bottom of every Testin dialog (#13).
     * <p>
     * A checkbox rather than a per-dialog control: it is one answer about the
     * tester, not twenty-eight answers about dialogs, and a tester who has
     * learned the keys wants the row gone from all of them at once.
     */
    private final @NotNull JBCheckBox showShortcutHintsBox = new JBCheckBox("Show keyboard shortcuts in dialogs");

    public SettingsConfigurable() {
        testinPathPanel = new TestinPathPanel();
        this.logLevelComboBox = new ComboBox<>(Arrays.stream(Level.values()).map(Level::name).toArray(String[]::new));
    }

    @Override
    public @NotNull String getDisplayName() {
        return Bundle.getPluginName();
    }

    /**
     * UC-SETTING-001, Rule-SETTING-040.
     * <p>
     * The same id plugin.xml registers the page under, which is what the
     * platform's settings search needs to open it and highlight the row that
     * matched.
     * <p>
     * A plain {@code Configurable} is findable as a page and no further: typing
     * a setting's name found Testin and left the tester to read eight rows for
     * the one they asked for (#124).
     */
    @Override
    public @NotNull String getId() {
        return "org.testin.setting.SettingsConfigurable";
    }

    // UC-SETTING-001
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
                .addVerticalGap(5)
                .addComponent(showShortcutHintsBox)
                .addVerticalGap(10)
                .addComponent(whereSettingsLive())
                .addComponentFillVertically(new JBPanel<>(), 0)
                .getPanel();
    }

    /**
     * UC-SETTING-001, Rule-SETTING-041.
     * <p>
     * Which of the two stores a value belongs to, said where a tester is
     * looking for one.
     * <p>
     * The split is practiced everywhere and stated nowhere the tester can see:
     * this page is the machine's and is never committed, and what a repository
     * says about itself is in its own file and travels with it. A tester
     * hunting for the test project on this page had no way to learn it is not
     * here (#124).
     */
    private @NotNull JBLabel whereSettingsLive() {
        final @NotNull JBLabel note = new JBLabel("<html>Everything here belongs to this machine and this person, and is never committed.<br>"
                + "Which test project a repository is about, and how it is shared, live in that repository's <b>testin.yml</b>.</html>");

        note.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        note.setFont(JBUI.Fonts.smallFont());

        return note;
    }

    // UC-SETTING-001, Rule-SETTING-008
    @Override
    public boolean isModified() {
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);
        boolean modified = !testinPathPanel.getPathText().trim().equals(settings.rootTestinPath);
        modified |= !Objects.equals(logLevelComboBox.getSelectedItem(), settings.logLevel);
        modified |= !testerNameField.getText().trim().equals(settings.testerName);
        modified |= !testerRoleField.getText().trim().equals(settings.testerRole);
        modified |= !downloadFolderField.getText().trim().equals(settings.defaultDownloadFolder);
        modified |= !sftpUserField.getText().equals(settings.sftpUser);
        modified |= !sftpKeyFileField.getText().equals(settings.sftpKeyFile);
        modified |= showShortcutHintsBox.isSelected() != settings.showShortcutHints;
        return modified;
    }

    /**
     * UC-SETTING-001, Rule-SETTING-042.
     * <p>
     * Refuses a Testin folder that is not one, before anything is stored.
     * <p>
     * Nothing on this page was checked. A path that does not exist, a path that
     * names a file, and a path of nothing but spaces were all stored exactly as
     * typed; the tree then showed its empty state, and nothing on screen
     * connected that to the path just entered - the tester was left to conclude
     * the plugin was broken (#237).
     * <p>
     * Thrown rather than notified, which is what {@code ConfigurationException}
     * is for: the settings dialog stays open with the message under the field,
     * so the value that cannot work is never stored in the first place. Empty is
     * allowed and always was - it is how a tester says they have not chosen yet,
     * and the panel has its own empty state for exactly that.
     */
    private void refuseAnImpossibleRoot() throws ConfigurationException {
        final @NotNull String typed = testinPathPanel.getPathText().trim();
        if (typed.isEmpty()) return;

        final @NotNull Path root = Path.of(typed);

        if (!Files.exists(root))
            throw new ConfigurationException("There is no folder at " + root + ".", "Testin Folder Not Found");

        if (!Files.isDirectory(root))
            throw new ConfigurationException(root + " is a file. The Testin folder has to be a folder, "
                    + "because test projects are folders inside it.", "Testin Folder Is Not A Folder");
    }

    // UC-SETTING-001, Rule-SETTING-009, Rule-SETTING-024, Rule-SETTING-042
    @Override
    public void apply() throws ConfigurationException {
        // Before a single field is read: a page that stored eight values and
        // then refused would leave seven of them applied.
        refuseAnImpossibleRoot();

        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);

        // Decided before the fields are overwritten: a moved root is the only change
        // that invalidates the tree, and re-indexing is far too heavy to run for a
        // renamed tester.
        final boolean rootChanged = TestinRoot.isRootChanged(settings.rootTestinPath, testinPathPanel.getPathText());

        // Trimmed here, like the two SFTP fields below. It used to be stored
        // exactly as typed and trimmed later by TestinRoot.normalize, so the
        // value in testinSettings.xml changed on its own at the next project
        // open - a stored setting nobody edited, different from yesterday
        // (#239).
        settings.rootTestinPath = testinPathPanel.getPathText().trim();
        settings.logLevel = Objects.requireNonNullElse((String) logLevelComboBox.getSelectedItem(),
                Level.INFO.name());
        settings.testerName = testerNameField.getText().trim();
        settings.testerRole = testerRoleField.getText().trim();
        settings.defaultDownloadFolder = downloadFolderField.getText().trim();
        settings.sftpUser = sftpUserField.getText().trim();
        settings.sftpKeyFile = sftpKeyFileField.getText().trim();
        settings.showShortcutHints = showShortcutHintsBox.isSelected();

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
            if (open.isDisposed() || Services.isNotCreated(open, TreePanel.class)) continue;

            Services.getInstance(open, TreePanel.class).reindex();
        }
    }

    // UC-SETTING-001
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
        showShortcutHintsBox.setSelected(settings.showShortcutHints);
    }

}
