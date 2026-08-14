package org.testin.importExport.exports;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.FileTypes;
import org.testin.enums.TestEditorAttributes;
import org.testin.importExport.shared.TablePanelBuilder;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.ui.dialogs.DestinationForm;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Shortcuts;

import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The export working dialog: destination form on top, the sheets to export
 * filling the middle, a visible Export button at the bottom — a working dialog
 * confirms by button, not by Enter; Escape cancels.
 */
public final class ExportDialog extends AbstractFrameworkDialog<DestinationForm> {

    private final @NotNull BiConsumer<@NotNull FileTypes, @NotNull File> onExport;

    public ExportDialog(final @NotNull Project p, final @NotNull List<TestEditorAttributes> exportAttributes,
                        final @NotNull Map<String, List<TestCaseDto>> sheetsData,
                        final @NotNull VirtualFile exportTarget,
                        final @NotNull BiConsumer<@NotNull FileTypes, @NotNull File> onExport) {
        super(p);
        this.onExport = onExport;

        title = "Export Test Cases";

        // Offer only formats that actually have an export handler (PDF/Word are report-only).
        final DestinationForm form = new DestinationForm(p,
                Arrays.stream(FileTypes.values()).filter(FileTypes::isExportable).toArray(FileTypes[]::new),
                FileTypes.XLSX,
                exportTarget.getName(),
                "Select Export Folder",
                "Choose the folder to save the export file in");

        components = List.of(
                ComponentDialogBase.of(form),
                ComponentDialogBase.panel(new TablePanelBuilder().createTabbedPane(sheetsData, exportAttributes, p), true),
                ComponentDialogBase.button("Export"));

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Escape, "Cancel", this::closeCancel));

        preferredSize = new Dimension(JBUI.scale(900), JBUI.scale(600));
    }

    @Override
    protected void submit() {
        final DestinationForm.Destination destination = component().resolve();
        if (destination == null) return;

        onExport.accept(destination.format(), destination.file());
        closeOk();
    }
}
