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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.FileTypes;
import org.testin.ui.framework.DialogComponent;

import javax.swing.JComponent;
import java.io.File;
import java.util.Arrays;
import java.util.Optional;

public final class DestinationForm implements DialogComponent {
    private static final boolean EXPANDED = true;

    private final @NotNull FolderSection folder;
    private final @NotNull FileNameSection fileName;
    private final @NotNull FormatSection format;
    private final @NotNull JComponent panel;

    // UC-REPORT-001, Rule-INTERNAL-099
    public static @NotNull DestinationForm stacked(final @NotNull Project p, final FileTypes @NotNull [] formats, final @NotNull FileTypes defaultFormat, final @NotNull String suggestedName, final @NotNull String chooserTitle, final @NotNull String chooserDescription) {
        return new DestinationForm(p, formats, defaultFormat, suggestedName, chooserTitle, chooserDescription, "");
    }

    // UC-SHARE-001, Rule-INTERNAL-099
    public static @NotNull DestinationForm inSection(final @NotNull Project p, final FileTypes @NotNull [] formats, final @NotNull FileTypes defaultFormat, final @NotNull String suggestedName, final @NotNull String chooserTitle, final @NotNull String chooserDescription, final @NotNull String section) {
        return new DestinationForm(p, formats, defaultFormat, suggestedName, chooserTitle, chooserDescription, section);
    }

    private DestinationForm(final @NotNull Project p, final FileTypes @NotNull [] formats, final @NotNull FileTypes defaultFormat, final @NotNull String suggestedName, final @NotNull String chooserTitle, final @NotNull String chooserDescription, final @NotNull String section) {
        folder = FolderSection.of(p, chooserTitle, chooserDescription);
        fileName = FileNameSection.of(suggestedName);
        format = FormatSection.of(formats, defaultFormat);

        final boolean roomy = !section.isEmpty();

        final @NotNull FormRows rows = roomy
                ? new FormRows().pair(folder.panel(), fileName.panel()).wideRow(format.panel())
                : new FormRows().wideRow(folder.panel()).wideRow(fileName.panel()).wideRow(format.panel());

        panel = roomy ? CollapsiblePanel.build(section, rows, EXPANDED) : DialogStyle.asSection(rows);
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

    // UC-SHARE-001
    public @NotNull Optional<Destination> resolve() {
        final @NotNull Optional<String> named = fileName.accepted();
        if (named.isEmpty()) return Optional.empty();

        final @NotNull Optional<String> chosenFolder = folder.accepted();
        if (chosenFolder.isEmpty()) return Optional.empty();

        final @NotNull FileTypes chosen = format.chosen();

        return Optional.of(new Destination(new File(chosenFolder.orElseThrow(), withExtension(named.orElseThrow(), chosen.getExtension())), chosen));
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return fileName.field();
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }
}
