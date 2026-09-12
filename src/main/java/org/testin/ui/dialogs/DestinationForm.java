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

import java.util.Optional;
import javax.swing.*;
import java.io.File;
import java.util.Arrays;

/**
 * Where a generated file goes: a destination folder, a file name, a format, and
 * the option to remember the folder for next time.
 * <p>
 * A framework dialog component, so the dialogs that write a file declare it as
 * content instead of laying the rows out themselves. It owns its own layout,
 * which is a component's business; the dialog owns the title, the button and
 * the status bar.
 */
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
        // The combo holds the format itself and renders its label, so the
        // selection needs no lookup back from text.
        //
        // Subclassed rather than built by SimpleListCellRenderer.create: both
        // factory overloads are deprecated on the 2026.2 branch and say they
        // will be removed, so an earlier fix that swapped one for the other
        // only moved the problem. The class is not going anywhere - customize
        // is its one abstract method - so implementing it is the form that
        // survives.
        //
        // The empty string is what an unselected combo renders, and the only
        // reason this reads a null at all; the model holds no nulls of its own.
        formatCombo.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(final @NotNull JList<? extends FileTypes> list, final FileTypes format, final int index, final boolean selected, final boolean focused) {
                setText(format == null ? "" : format.getLabel());
            }
        });

        final @NotNull FileChooserDescriptor descriptor = FileChooserDescriptorFactory
                .createSingleFolderDescriptor()
                .withTitle(chooserTitle)
                .withDescription(chooserDescription);

        folderField.addBrowseFolderListener(p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        // Seeded, never opened: the dialog already shows what it will write, so
        // the tester browses when they want to. Only the import dialog, which
        // has nothing to show until a file is chosen, opens its chooser itself.
        folderField.setText(defaultFolder());

        rows = buildRows();
    }

    /**
     * The name with the chosen format's extension on it.
     * <p>
     * Only a tail that is itself a known extension is replaced. Cutting at the
     * last dot regardless turned "Sprint 1.2 Report" into "Sprint 1.pdf" - the
     * file was written, under a name the tester did not choose.
     */
    // Package-private rather than private so the naming rule can be tested
    // without a Project and a Swing form behind it.
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

    /**
     * UC-SHARE-001.
     * <p>
     * The destination, or empty when a field is still empty - in which case the
     * offending field takes the focus and the dialog stays open.
     */
    public @NotNull Optional<Destination> resolve() {
        final @NotNull String folder = folderField.getText().trim();
        final @NotNull String fileName = fileNameField.getText().trim();

        // Said in the box that is empty, the way every dialog on the framework
        // says it. All three used to move the cursor and nothing else: the
        // tester pressed the button, the dialog stayed open, nothing turned red
        // and nothing was written - which reads as a button that does not work
        // rather than as a field that needs filling in (#251).
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

    /**
     * Rule-SETTING-021, Rule-SETTING-023.
     * <p>
     * Where the dialog starts, never where it writes. This form used to offer a
     * "Set as default folder" tick box that stored the chosen folder, so a
     * setting the tester had made on the settings page was overwritten from a
     * dialog - and the box was drawn only while no folder was set, so once one
     * was there it could not be changed back from here (#240).
     */
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
        // The dialog confirms by its Export button, not by Enter in a field.
    }

    /**
     * A resolved destination. Only produced when every field is filled, so the
     * caller never has to re-check them.
     */
    public record Destination(@NotNull File file, @NotNull FileTypes format) {
    }
}
