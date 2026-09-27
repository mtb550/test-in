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

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;
import org.testin.ui.framework.EmptyWarning;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import java.util.Optional;

public record FolderSection(@NotNull TextFieldWithBrowseButton field) {
    // Rule-SETTING-021, Rule-SETTING-023
    public static @NotNull FolderSection of(final @NotNull Project p, final @NotNull String chooserTitle, final @NotNull String chooserDescription) {
        final @NotNull TextFieldWithBrowseButton field = new TextFieldWithBrowseButton();
        DialogStyle.asField(field.getTextField());
        // Rule-INTERNAL-096
        DialogStyle.framed(field.getTextField());

        field.addBrowseFolderListener(p, FileChooserDescriptorFactory.singleDir().withTitle(chooserTitle).withDescription(chooserDescription), TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        field.setText(DownloadFolder.of(p));

        return new FolderSection(field);
    }

    // Rule-INTERNAL-087
    public @NotNull JComponent panel() {
        return Caption.above(Bundle.message("destination.caption.folder"), field);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-067
    public @NotNull Optional<String> accepted() {
        final @NotNull String folder = field.getText().trim();
        if (!folder.isEmpty()) return Optional.of(folder);

        EmptyWarning.show(field.getTextField(), Bundle.message("destination.choose.folder"));
        return Optional.empty();
    }
}
