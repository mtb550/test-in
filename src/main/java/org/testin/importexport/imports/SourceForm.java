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

package org.testin.importexport.imports;

import org.testin.util.Html;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.FileTypes;
import org.testin.importexport.shared.FileDocumentListener;
import org.testin.testcase.TestEditorAttributes;
import org.testin.testcase.TestEditorAttributes.Can;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.ui.dialogs.FormRows;
import org.testin.ui.framework.DialogComponent;
import org.testin.util.Bundle;

import java.util.Optional;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
                .withExtensionFilter("", FileTypes.importableExtensionsForChooser())
                .withTitle(Bundle.message("import.file.title"))
                .withDescription(Bundle.message("import.file.description"));

        fileField.addBrowseFolderListener(p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        fileField.getTextField().getDocument().addDocumentListener(
                new FileDocumentListener(fileField, p, this::showStatus, (format, parsedData) -> {
                    showFormatHint(format);
                    onDataLoaded.accept(parsedData);
                }, importLoader));

        // UC-SHARE-007, Rule-SHARE-036
        fileField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final @NotNull DocumentEvent e) {
                onDataLoaded.accept(Map.of());
            }
        });

        formatHint.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        formatHint.setVisible(false);

        rows = new FormRows().row(Bundle.message("import.caption.source"), fileField);
        rows.wideRow(formatHint);
    }

    // UC-SHARE-005, Rule-SETTING-021
    public void selectSourceFile() {
        fileField.setText(defaultFolder());

        final @NotNull ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> browseListener =
                new ComponentWithBrowseButton.BrowseFolderActionListener<>(fileField, p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        ApplicationManager.getApplication().invokeLater(() ->
                browseListener.actionPerformed(new ActionEvent(fileField.getTextField(), ActionEvent.ACTION_PERFORMED, "browse")));
    }

    // UC-SHARE-005
    public @NotNull Optional<File> resolve() {
        final @NotNull String filePath = fileField.getText().trim();
        if (filePath.isEmpty()) {
            fileField.getTextField().requestFocus();
            return Optional.empty();
        }

        return Optional.of(new File(filePath));
    }

    // UC-SHARE-005, Rule-SHARE-107
    private void showStatus(final @NotNull String status) {
        formatHint.setText(status);
        formatHint.setVisible(!status.isBlank());
    }

    private void showFormatHint(final @NotNull FileTypes format) {
        final @NotNull String columns = importAttributes.stream()
                .filter(a -> a.can(Can.IMPORT))
                .map(TestEditorAttributes::getName)
                .collect(Collectors.joining(", "));

        final @NotNull String hint = format.hintFor(columns);
        if (hint.isBlank()) {
            formatHint.setVisible(false);
            return;
        }

        final @NotNull String escaped = Html.ofText(hint);
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
    }
}
