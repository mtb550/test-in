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

package org.testin.ui.dialogs;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.FileTypes;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.ui.framework.DialogComponent;
import org.testin.ui.framework.EmptyWarning;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import javax.swing.JList;
import java.io.File;
import java.util.Arrays;
import java.util.Optional;

public final class DestinationForm implements DialogComponent {
    private final @NotNull Project p;
    private final @NotNull FormRows rows;
    private final @NotNull TextFieldWithBrowseButton folderField = new TextFieldWithBrowseButton();
    private final @NotNull JBTextField fileNameField = new JBTextField(30);
    private final @NotNull ComboBox<FileTypes> formatCombo;

    public DestinationForm(final @NotNull Project p, final FileTypes @NotNull [] formats, final @NotNull FileTypes defaultFormat, final @NotNull String fileName, final @NotNull String chooserTitle, final @NotNull String chooserDescription) {
        this.p = p;
        this.formatCombo = new ComboBox<>(formats);

        fileNameField.setText(fileName);
        formatCombo.setSelectedItem(defaultFormat);
        formatCombo.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(final @NotNull JList<? extends FileTypes> list, final FileTypes format, final int index, final boolean selected, final boolean focused) {
                setText(format == null ? "" : format.getLabel());
            }
        });

        final @NotNull FileChooserDescriptor descriptor = FileChooserDescriptorFactory
                .singleDir()
                .withTitle(chooserTitle)
                .withDescription(chooserDescription);

        folderField.addBrowseFolderListener(p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        folderField.setText(defaultFolder());

        rows = buildRows();
    }

    static @NotNull String withExtension(final @NotNull String fileName, final @NotNull String extension) {
        if (fileName.endsWith(extension)) return fileName;

        final int dot = fileName.lastIndexOf('.');
        if (dot < 0) return fileName + extension;

        final @NotNull String tail = fileName.substring(dot);
        final boolean tailIsAnExtension = Arrays.stream(FileTypes.values())
                .anyMatch(type -> type.getExtension().equalsIgnoreCase(tail));

        return tailIsAnExtension ? fileName.substring(0, dot) + extension : fileName + extension;
    }

    private @NotNull FormRows buildRows() {
        final @NotNull FormRows formRows = new FormRows()
                .row(Bundle.message("destination.caption.folder"), folderField)
                .row(Bundle.message("destination.caption.file"), fileNameField)
                .row(Bundle.message("destination.caption.format"), formatCombo);

        return formRows;
    }

    // UC-SHARE-001
    public @NotNull Optional<Destination> resolve() {
        final @NotNull String folder = folderField.getText().trim();
        final @NotNull String fileName = fileNameField.getText().trim();

        if (fileName.isEmpty()) {
            EmptyWarning.show(fileNameField, Bundle.message("destination.name.the.file"));
            return Optional.empty();
        }
        if (folder.isEmpty()) {
            EmptyWarning.show(folderField.getTextField(), Bundle.message("destination.choose.folder"));
            return Optional.empty();
        }

        final @NotNull Optional<FileTypes> selectedFormat = Optional.ofNullable((FileTypes) formatCombo.getSelectedItem());
        if (selectedFormat.isEmpty()) {
            EmptyWarning.show(formatCombo, Bundle.message("destination.choose.format"));
            return Optional.empty();
        }

        final @NotNull FileTypes format = selectedFormat.orElseThrow();

        return Optional.of(new Destination(new File(folder, withExtension(fileName, format.getExtension())), format));
    }

    // Rule-SETTING-021, Rule-SETTING-023
    private @NotNull String defaultFolder() {
        return Services.getInstance(p, AppSettingsState.class).defaultDownloadFolder;
    }

    @Override
    public @NotNull JComponent getPanel() {
        return rows;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return fileNameField;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    public record Destination(@NotNull File file, @NotNull FileTypes format) {
    }
}
