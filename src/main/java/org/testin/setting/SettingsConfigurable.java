/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.testin.util.Fonts;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

public final class SettingsConfigurable implements SearchableConfigurable {
    private final @NotNull TestinPathPanel testinPathPanel;
    private final @NotNull JBTextField testerNameField = new JBTextField();
    private final @NotNull JBTextField testerRoleField = new JBTextField();
    private final @NotNull TextFieldWithBrowseButton downloadFolderField = new TextFieldWithBrowseButton();

    private final @NotNull ComboBox<String> logLevelComboBox;

    private final @NotNull JBCheckBox showShortcutHintsBox = new JBCheckBox(Bundle.message("settings.show.shortcuts"));

    public SettingsConfigurable() {
        testinPathPanel = new TestinPathPanel();
        this.logLevelComboBox = new ComboBox<>(Arrays.stream(Level.values()).map(Level::name).toArray(String[]::new));
    }

    @Override
    public @NotNull String getDisplayName() {
        return Bundle.getPluginName();
    }

    // UC-SETTING-001, Rule-SETTING-040
    @Override
    public @NotNull String getId() {
        return "org.testin.setting.SettingsConfigurable";
    }

    // UC-SETTING-001
    @Override
    public @NotNull JComponent createComponent() {
        downloadFolderField.addBrowseFolderListener(null, FileChooserDescriptorFactory.singleDir()
                        .withTitle(Bundle.message("settings.download.folder.title"))
                        .withDescription(Bundle.message("settings.download.folder.description")),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel(Bundle.message("settings.label.source.root")), testinPathPanel.getComponent(), 1, false)
                .addVerticalGap(5)
                .addLabeledComponent(Bundle.message("settings.label.log.level"), logLevelComboBox)
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel(Bundle.message("settings.label.tester.name")), testerNameField, 1, false)
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel(Bundle.message("settings.label.tester.role")), testerRoleField, 1, false)
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel(Bundle.message("settings.label.download.folder")), downloadFolderField, 1, false)
                .addVerticalGap(5)
                .addComponent(showShortcutHintsBox)
                .addVerticalGap(10)
                .addComponent(whereSettingsLive())
                .addComponentFillVertically(new JBPanel<>(), 0)
                .getPanel();
    }

    // UC-SETTING-001, Rule-SETTING-041
    private @NotNull JBLabel whereSettingsLive() {
        final @NotNull JBLabel note = new JBLabel(Bundle.message("settings.note"));

        note.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        note.setFont(Fonts.small());

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
        modified |= showShortcutHintsBox.isSelected() != settings.showShortcutHints;
        return modified;
    }

    // UC-SETTING-001, Rule-SETTING-042
    private void refuseAnImpossibleRoot() throws ConfigurationException {
        final @NotNull String typed = testinPathPanel.getPathText().trim();
        if (typed.isEmpty()) return;

        final @NotNull Path root;
        try {
            root = Path.of(typed);
        } catch (final InvalidPathException notAPath) {
            Logger.info("The Testin folder typed is not a path: " + notAPath.getMessage());
            throw new ConfigurationException(Bundle.message("settings.no.folder", typed), Bundle.message("settings.no.folder.title"));
        }

        // Rule-SETTING-013
        if (!root.isAbsolute())
            throw new ConfigurationException(Bundle.message("settings.not.absolute", root), Bundle.message("settings.not.absolute.title"));

        if (!Files.exists(root))
            throw new ConfigurationException(Bundle.message("settings.no.folder", root), Bundle.message("settings.no.folder.title"));

        if (!Files.isDirectory(root))
            throw new ConfigurationException(Bundle.message("settings.not.a.folder", root), Bundle.message("settings.not.a.folder.title"));
    }

    // UC-SETTING-001, Rule-SETTING-009, Rule-SETTING-024, Rule-SETTING-042
    @Override
    public void apply() throws ConfigurationException {
        refuseAnImpossibleRoot();

        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);

        final boolean rootChanged = TestinRoot.isRootChanged(settings.rootTestinPath, testinPathPanel.getPathText());

        settings.rootTestinPath = testinPathPanel.getPathText().trim();
        settings.logLevel = Objects.requireNonNullElse((String) logLevelComboBox.getSelectedItem(),
                Level.INFO.name());
        settings.testerName = testerNameField.getText().trim();
        settings.testerRole = testerRoleField.getText().trim();
        settings.defaultDownloadFolder = downloadFolderField.getText().trim();
        settings.showShortcutHints = showShortcutHintsBox.isSelected();

        Logger.setLogLevel(Level.valueOf(settings.logLevel));

        if (rootChanged) refreshEveryOpenProject();
    }

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
        showShortcutHintsBox.setSelected(settings.showShortcutHints);
    }
}
