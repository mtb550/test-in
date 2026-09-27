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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.FileTypes;
import org.testin.ui.Caption;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.ui.dialogs.DownloadFolder;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Optional;

public record SourceSection(@NotNull TextFieldWithBrowseButton field, @NotNull FileChooserDescriptor descriptor) {
    // Rule-INTERNAL-095, Rule-INTERNAL-096
    public static @NotNull SourceSection of(final @NotNull Project p) {
        final @NotNull TextFieldWithBrowseButton field = new TextFieldWithBrowseButton();
        DialogStyle.asField(field.getTextField());
        DialogStyle.framed(field.getTextField());

        final @NotNull FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withExtensionFilter("", FileTypes.importableExtensionsForChooser())
                .withTitle(Bundle.message("import.file.title"))
                .withDescription(Bundle.message("import.file.description"));

        field.addBrowseFolderListener(p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        return new SourceSection(field, descriptor);
    }

    // Rule-INTERNAL-087
    public @NotNull JComponent panel() {
        return Caption.above(Bundle.message("import.caption.source"), field);
    }

    // UC-SHARE-005, Rule-SETTING-021
    public void browse(final @NotNull Project p) {
        field.setText(DownloadFolder.of(p));

        final @NotNull ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> browsing =
                new ComponentWithBrowseButton.BrowseFolderActionListener<>(field, p, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        ApplicationManager.getApplication().invokeLater(() ->
                browsing.actionPerformed(new ActionEvent(field.getTextField(), ActionEvent.ACTION_PERFORMED, "browse")));
    }

    // UC-SHARE-005
    public @NotNull Optional<File> accepted() {
        final @NotNull String path = field.getText().trim();
        if (!path.isEmpty()) return Optional.of(new File(path));

        field.getTextField().requestFocus();
        return Optional.empty();
    }
}
