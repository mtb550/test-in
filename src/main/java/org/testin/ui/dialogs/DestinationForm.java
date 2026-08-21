package org.testin.ui.dialogs;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.FileTypes;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.ui.framework.DialogComponent;

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
    private final @NotNull JBCheckBox setDefaultCheckBox = new JBCheckBox("Set as default folder");
    /**
     * Whether the remember-this-folder checkbox is offered, decided once.
     * Drawing the row and writing the setting used to derive it separately, so
     * one could show the box and the other ignore the answer.
     */
    private final boolean offersDefaultFolder;

    public DestinationForm(final @NotNull Project p, final FileTypes @NotNull [] formats,
                           final @NotNull FileTypes defaultFormat, final @NotNull String fileName,
                           final @NotNull String chooserTitle, final @NotNull String chooserDescription) {
        this.p = p;
        this.formatCombo = new ComboBox<>(formats);
        this.offersDefaultFolder = defaultFolder().isBlank();

        fileNameField.setText(fileName);
        formatCombo.setSelectedItem(defaultFormat);
        // The combo holds the format itself and renders its label, so the
        // selection needs no lookup back from text.
        formatCombo.setRenderer(SimpleListCellRenderer.create("", FileTypes::getLabel));

        final FileChooserDescriptor descriptor = FileChooserDescriptorFactory
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

        final String tail = fileName.substring(dot);
        final boolean tailIsAnExtension = Arrays.stream(FileTypes.values())
                .anyMatch(type -> type.getExtension().equalsIgnoreCase(tail));

        return tailIsAnExtension ? fileName.substring(0, dot) + extension : fileName + extension;
    }

    private @NotNull FormRows buildRows() {
        final FormRows formRows = new FormRows()
                .row("Destination:", folderField)
                .row("File name:", fileNameField)
                .row("Format:", formatCombo);

        if (offersDefaultFolder)
            formRows.unlabeledRow(setDefaultCheckBox);

        return formRows;
    }

    /**
     * The destination, or empty when a field is still empty - in which case the
     * offending field takes the focus and the dialog stays open. Remembers the
     * folder when the checkbox is ticked.
     */
    public @NotNull Optional<Destination> resolve() {
        final String folder = folderField.getText().trim();
        final String fileName = fileNameField.getText().trim();

        if (fileName.isEmpty()) {
            fileNameField.requestFocus();
            return Optional.empty();
        }
        if (folder.isEmpty()) {
            folderField.getTextField().requestFocus();
            return Optional.empty();
        }

        final Optional<FileTypes> selectedFormat = Optional.ofNullable((FileTypes) formatCombo.getSelectedItem());
        if (selectedFormat.isEmpty()) {
            formatCombo.requestFocus();
            return Optional.empty();
        }

        final FileTypes format = selectedFormat.orElseThrow();

        if (offersDefaultFolder && setDefaultCheckBox.isSelected())
            Services.getInstance(p, AppSettingsState.class).defaultDownloadFolder = folder;

        return Optional.of(new Destination(new File(folder, withExtension(fileName, format.getExtension())), format));
    }

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
