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

package org.testin.importexport.exports;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.FileTypes;
import org.testin.importexport.shared.SheetPreview;
import org.testin.testcase.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.dialogs.DestinationForm;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The export working dialog: destination form on top, the sheets to export
 * filling the middle, a visible Export button at the bottom. Enter exports and
 * Escape cancels, and the status bar names both.
 * <p>
 * What the tester ticks is what gets written. The preview used to draw its
 * checkboxes and then export everything regardless.
 */
public final class ExportDialog extends AbstractFrameworkDialog<DestinationForm> {

    private final @NotNull SheetPreview preview;
    private final @NotNull BiConsumer<DestinationForm.@NotNull Destination,
            @NotNull Map<String, List<TestCaseDto>>> onExport;

    public ExportDialog(final @NotNull Project p, final @NotNull List<TestEditorAttributes> exportAttributes, final @NotNull Map<String, List<TestCaseDto>> sheetsData, final @NotNull VirtualFile exportTarget, final @NotNull BiConsumer<DestinationForm.@NotNull Destination, @NotNull Map<String, List<TestCaseDto>>> onExport) {
        super(p);
        this.onExport = onExport;

        title = Bundle.message("dialog.export.title");

        // Offer only formats that actually have an export handler (PDF/Word are report-only).
        final @NotNull DestinationForm form = new DestinationForm(p,
                Arrays.stream(FileTypes.values()).filter(FileTypes::isExportable).toArray(FileTypes[]::new),
                FileTypes.XLSX,
                exportTarget.getName(),
                Bundle.message("dialog.export.folder.title"),
                Bundle.message("dialog.export.folder.message"));

        preview = new SheetPreview(p, exportAttributes);
        preview.show(sheetsData);

        components = List.of(
                ComponentDialogBase.of(form),
                ComponentDialogBase.of(preview),
                ComponentDialogBase.button(ExportAction.NAME));

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, ExportAction.NAME, this::submit),
                StatusBarShortcut.cancel(this::closeCancel));

        preferredSize = new Dimension(JBUI.scale(900), JBUI.scale(600));
    }

    // UC-SHARE-001
    @Override
    protected void submit() {
        component().resolve().ifPresent(destination -> {
            final @NotNull Map<String, List<TestCaseDto>> selected = preview.selected();
            if (selected.isEmpty()) {
                Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("notification.export.empty.title"), Bundle.message("notification.export.empty.message"));
                return;
            }

            // Both the destination and the selection are values already, so
            // the dialog can go before anything is written (#87).
            closeOk();
            onExport.accept(destination, selected);
        });
    }
}
