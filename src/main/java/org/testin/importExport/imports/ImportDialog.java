package org.testin.importExport.imports;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.FileTypes;
import org.testin.enums.TestEditorAttributes;
import org.testin.importExport.shared.SheetPreview;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Shortcuts;

import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * The import working dialog: source form on top, one preview tab per sheet
 * filling the middle, a visible Import button at the bottom. Reports the cases
 * the tester ticked through its callback, so a canceled dialog can never be
 * read back for a selection.
 */
public final class ImportDialog extends AbstractFrameworkDialog<SourceForm> {

    private final @NotNull Consumer<@NotNull Map<String, List<TestCaseDto>>> onImport;

    // Created with the dialog, not on show: the file listener can load data as
    // soon as the source field is filled.
    private final @NotNull SheetPreview preview;

    public ImportDialog(final @NotNull Project p, final @NotNull List<TestEditorAttributes> importAttributes,
                        final @NotNull BiFunction<File, FileTypes, Map<String, List<TestCaseDto>>> importLoader,
                        final @NotNull Consumer<@NotNull Map<String, List<TestCaseDto>>> onImport) {
        super(p);
        this.onImport = onImport;

        title = "Import Test Cases";

        preview = new SheetPreview(p, importAttributes);
        final SourceForm form = new SourceForm(p, importAttributes, importLoader, preview::show);

        components = List.of(
                ComponentDialogBase.of(form),
                ComponentDialogBase.of(preview),
                ComponentDialogBase.button("Import"));

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Escape, "Cancel", this::closeCancel));

        preferredSize = new Dimension(JBUI.scale(900), JBUI.scale(600));

        form.selectSourceFile();
    }

    @Override
    protected void submit() {
        // The form validates and focuses its own field; what the import
        // consumes is the parsed data below.
        if (component().resolve() == null) return;

        if (preview.isEmpty()) {
            Services.getInstance(p, Notifier.class).error(p, "Import Error", "No data loaded from the selected file.");
            return;
        }

        final Map<String, List<TestCaseDto>> selected = preview.selected();
        if (selected.isEmpty()) {
            Services.getInstance(p, Notifier.class).error(p, "Import Error", "Please select at least one test case to import.");
            return;
        }

        onImport.accept(selected);
        closeOk();
    }

}
