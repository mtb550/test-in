package org.testin.importexport.imports;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.FileTypes;
import org.testin.importexport.shared.FileDocumentListener;
import org.testin.model.TestEditorAttributes;
import org.testin.model.TestEditorAttributes.Can;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.ui.dialogs.FormRows;
import org.testin.ui.framework.DialogComponent;
import org.testin.util.Bundle;

import java.util.Optional;
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
    private final @NotNull JBLabel formatHint = new JBLabel();
    private final @NotNull FileChooserDescriptor descriptor;

    public SourceForm(final @NotNull Project p, final @NotNull List<TestEditorAttributes> importAttributes, final @NotNull BiFunction<File, FileTypes, Map<String, List<TestCaseDto>>> importLoader, final @NotNull Consumer<@NotNull Map<String, List<TestCaseDto>>> onDataLoaded) {
        this.p = p;
        this.importAttributes = importAttributes;

        descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withExtensionFilter("", "xls", "xlsx", "csv", "json")
                .withTitle(Bundle.message("import.file.title"))
                .withDescription(Bundle.message("import.file.description"));

        fileField.addBrowseFolderListener(p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        // The form shows the hint for whatever format was just parsed; the
        // dialog only ever hears about the data.
        fileField.getTextField().getDocument().addDocumentListener(
                new FileDocumentListener(fileField, p, this::showStatus, (format, parsedData) -> {
                    showFormatHint(format);
                    onDataLoaded.accept(parsedData);
                }, importLoader));

        formatHint.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        formatHint.setVisible(false);

        rows = new FormRows().row(Bundle.message("import.caption.source"), fileField);
        rows.wideRow(formatHint);
    }

    /**
     * UC-SHARE-005, Rule-SETTING-021.
     * <p>
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
        final @NotNull ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> browseListener =
                new ComponentWithBrowseButton.BrowseFolderActionListener<>(fileField, p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        ApplicationManager.getApplication().invokeLater(() ->
                browseListener.actionPerformed(new ActionEvent(fileField.getTextField(), ActionEvent.ACTION_PERFORMED, "browse")));
    }

    /**
     * UC-SHARE-005.
     * <p>
     * The chosen file, or empty when the field is still empty - in which case
     * it takes the focus and the dialog stays open.
     */
    public @NotNull Optional<File> resolve() {
        final @NotNull String filePath = fileField.getText().trim();
        if (filePath.isEmpty()) {
            fileField.getTextField().requestFocus();
            return Optional.empty();
        }

        return Optional.of(new File(filePath));
    }

    /**
     * The format's hint, with the importable column names filled in. Built from
     * the attributes the form was given, so it can never list a column the
     * import would ignore.
     */
    /**
     * UC-SHARE-005, Rule-SHARE-107.
     * <p>
     * What the form says while it is reading a file, on the row the format hint
     * uses when it has one.
     * <p>
     * One row rather than two: the two never have anything to say at the same
     * moment - the hint describes a file that has been read, and this describes
     * one being read - and a second line that is blank most of the time is a
     * gap in the form for nothing.
     */
    private void showStatus(final @NotNull String status) {
        formatHint.setText(status);
        formatHint.setVisible(!status.isBlank());
    }

    private void showFormatHint(final @NotNull FileTypes format) {
        final @NotNull String message = format.getInfoMessage();
        if (message.isBlank()) {
            formatHint.setVisible(false);
            return;
        }

        final @NotNull String columns = importAttributes.stream()
                .filter(a -> a.can(Can.IMPORT))
                .map(TestEditorAttributes::getName)
                .collect(Collectors.joining(", "));

        final @NotNull String escaped = StringUtil.escapeXmlEntities(message.formatted(columns)).replace("\n", "<br>");
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
