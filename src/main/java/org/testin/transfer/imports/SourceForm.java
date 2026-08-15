package org.testin.transfer.imports;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.FileTypes;
import org.testin.enums.TestEditorAttributes;
import org.testin.transfer.shared.FileDocumentListener;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.settings.AppSettingsState;
import org.testin.ui.dialogs.FormRows;
import org.testin.ui.framework.DialogComponent;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Where imported test cases come from: a source file, the option to remember
 * its folder, and the chosen format's hint about the columns the file needs.
 * <p>
 * The mirror of {@code DestinationForm} for dialogs that read a file instead of
 * writing one. It lives beside the import code rather than in {@code ui.dialogs}
 * because the hint it renders is built from the importable attributes - that is
 * an import concern, not a dialog one.
 */
public final class SourceForm implements DialogComponent {

    private final @NotNull Project p;
    private final @NotNull List<TestEditorAttributes> importAttributes;
    private final @NotNull FormRows rows;

    private final @NotNull TextFieldWithBrowseButton fileField = new TextFieldWithBrowseButton();
    private final @NotNull JBCheckBox setDefaultCheckBox = new JBCheckBox("Set as default folder");
    private final @NotNull JBLabel formatHint = new JBLabel();
    private final @NotNull FileChooserDescriptor descriptor;

    /**
     * Whether the remember-this-folder checkbox is offered, decided once -
     * drawing the row and writing the setting used to derive it separately.
     */
    private final boolean offersDefaultFolder;

    public SourceForm(final @NotNull Project p, final @NotNull List<TestEditorAttributes> importAttributes,
                      final @NotNull BiFunction<File, FileTypes, Map<String, List<TestCaseDto>>> importLoader,
                      final @NotNull Consumer<@NotNull Map<String, List<TestCaseDto>>> onDataLoaded) {
        this.p = p;
        this.importAttributes = importAttributes;
        this.offersDefaultFolder = defaultFolder().isBlank();

        descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withExtensionFilter("", "xls", "xlsx", "csv", "json")
                .withTitle("Select Import File")
                .withDescription("Choose a file to import test cases from (.xls, .xlsx, .json, .csv)");

        fileField.addBrowseFolderListener(p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        // The form shows the hint for whatever format was just parsed; the
        // dialog only ever hears about the data.
        fileField.getTextField().getDocument().addDocumentListener(
                new FileDocumentListener(fileField, p, (format, parsedData) -> {
                    showFormatHint(format);
                    onDataLoaded.accept(parsedData);
                }, importLoader));

        formatHint.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        formatHint.setVisible(false);

        rows = new FormRows().row("Source:", fileField);
        if (offersDefaultFolder) rows.row("Options:", setDefaultCheckBox);
        rows.wideRow(formatHint);
    }

    /**
     * Opens the chooser as soon as the dialog is on screen - the import dialog
     * has nothing to preview until a file is picked, so it asks for one instead
     * of waiting. The default folder, when set, seeds the field so the chooser
     * starts there. Deferred, so the chooser opens over a dialog that is
     * already up.
     */
    public void selectSourceFile() {
        fileField.setText(defaultFolder());

        // Fired directly, not registered: addBrowseFolderListener above already
        // owns the button, and registering this one too opened the chooser a
        // second time as soon as the first closed.
        final ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> browseListener =
                new ComponentWithBrowseButton.BrowseFolderActionListener<>(fileField, p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        ApplicationManager.getApplication().invokeLater(() ->
                browseListener.actionPerformed(new ActionEvent(fileField.getTextField(), ActionEvent.ACTION_PERFORMED, "browse")));
    }

    /**
     * The chosen file, or null when the field is still empty - in which case it
     * takes the focus and the dialog stays open. Remembers the file's folder
     * when the checkbox is ticked.
     */
    public @Nullable File resolve() {
        final String filePath = fileField.getText().trim();
        if (filePath.isEmpty()) {
            fileField.getTextField().requestFocus();
            return null;
        }

        final File source = new File(filePath);

        if (offersDefaultFolder && setDefaultCheckBox.isSelected()) {
            final File folder = source.getParentFile();
            if (folder != null)
                Services.getInstance(p, AppSettingsState.class).defaultDownloadFolder = folder.getAbsolutePath();
        }

        return source;
    }

    /**
     * The format's hint, with the importable column names filled in. Built from
     * the attributes the form was given, so it can never list a column the
     * import would ignore.
     */
    private void showFormatHint(final @NotNull FileTypes format) {
        final String message = format.getInfoMessage();
        if (message.isBlank()) {
            formatHint.setVisible(false);
            return;
        }

        final String columns = importAttributes.stream()
                .filter(TestEditorAttributes::isImportable)
                .map(TestEditorAttributes::getName)
                .collect(Collectors.joining(", "));

        final String escaped = StringUtil.escapeXmlEntities(message.formatted(columns)).replace("\n", "<br>");
        formatHint.setText("<html>" + escaped + "</html>");
        formatHint.setVisible(true);
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
        return fileField.getTextField();
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
        // The dialog confirms by its Import button, not by Enter in a field.
    }
}
