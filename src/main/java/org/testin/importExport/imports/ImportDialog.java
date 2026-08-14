package org.testin.importExport.imports;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.FileTypes;
import org.testin.enums.TestEditorAttributes;
import org.testin.importExport.shared.CellEditListener;
import org.testin.importExport.shared.TablePanelBuilder;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Shortcuts;

import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * The import working dialog: source form on top, one preview tab per sheet
 * filling the middle, a visible Import button at the bottom. Reports the cases
 * the tester ticked through its callback, so a cancelled dialog can never be
 * read back for a selection.
 */
public final class ImportDialog extends AbstractFrameworkDialog<SourceForm> {

    private final @NotNull List<TestEditorAttributes> importAttributes;
    private final @NotNull Consumer<@NotNull Map<String, List<TestCaseDto>>> onImport;

    // Created with the dialog, not on show: the file listener can load data as
    // soon as the source field is filled.
    private final @NotNull JBTabbedPane sheetTabs = new JBTabbedPane();
    private final @NotNull Map<String, DefaultTableModel> tableModels = new LinkedHashMap<>();

    private @NotNull Map<String, List<TestCaseDto>> sheetsData = new LinkedHashMap<>();

    public ImportDialog(final @NotNull Project p, final @NotNull List<TestEditorAttributes> importAttributes,
                        final @NotNull BiFunction<File, FileTypes, Map<String, List<TestCaseDto>>> importLoader,
                        final @NotNull Consumer<@NotNull Map<String, List<TestCaseDto>>> onImport) {
        super(p);
        this.importAttributes = importAttributes;
        this.onImport = onImport;

        title = "Import Test Cases";

        final SourceForm form = new SourceForm(p, importAttributes, importLoader, this::onDataLoaded);

        components = List.of(
                ComponentDialogBase.of(form),
                ComponentDialogBase.panel(sheetTabs, true),
                ComponentDialogBase.button("Import"));

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Escape, "Cancel", this::cancel));

        preferredSize = new Dimension(JBUI.scale(900), JBUI.scale(600));

        form.selectSourceFile();
    }

    @Override
    protected void submit() {
        // The form validates and focuses its own field; what the import
        // consumes is the parsed data below.
        if (component().resolve() == null) return;

        if (sheetsData.isEmpty()) {
            Services.getInstance(p, Notifier.class).error(p, "Import Error", "No data loaded from the selected file.");
            return;
        }

        final Map<String, List<TestCaseDto>> selected = selectedBySheet();
        if (selected.isEmpty()) {
            Services.getInstance(p, Notifier.class).error(p, "Import Error", "Please select at least one test case to import.");
            return;
        }

        onImport.accept(selected);
        closeOk();
    }

    private void cancel() {
        Services.getInstance(p, Notifier.class).softShow(p, "Import Cancelled", "Import was cancelled from preview dialog.");
        closeCancel();
    }

    /**
     * Rebuilds the preview for a newly parsed file. The models go with the
     * tabs: keeping the previous file's models would leave rows nobody can see
     * still selectable.
     */
    private void onDataLoaded(final @NotNull Map<String, List<TestCaseDto>> parsedData) {
        sheetsData = parsedData;

        tableModels.clear();
        while (sheetTabs.getTabCount() > 0) {
            sheetTabs.removeTabAt(0);
        }

        for (final Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
            final List<TestCaseDto> testCases = entry.getValue();

            final DefaultTableModel model = new TablePanelBuilder().createModel(p, importAttributes, testCases);
            model.addTableModelListener(new CellEditListener(importAttributes, p, testCases));

            tableModels.put(entry.getKey(), model);
            sheetTabs.addTab(entry.getKey(), new JBScrollPane(new TablePanelBuilder().buildTable(model, p)));
        }
    }

    /**
     * The ticked cases per sheet; sheets with nothing ticked are left out, so
     * an empty result means the tester selected nothing at all.
     */
    private @NotNull Map<String, List<TestCaseDto>> selectedBySheet() {
        final Map<String, List<TestCaseDto>> selectedBySheet = new LinkedHashMap<>();

        for (final Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
            final DefaultTableModel model = tableModels.get(entry.getKey());
            if (model == null) continue;

            final List<TestCaseDto> casesInSheet = entry.getValue();
            final List<TestCaseDto> selected = new ArrayList<>();

            for (int row = 0; row < model.getRowCount(); row++) {
                if (Boolean.TRUE.equals(model.getValueAt(row, 0))) selected.add(casesInSheet.get(row));
            }

            if (!selected.isEmpty()) selectedBySheet.put(entry.getKey(), selected);
        }

        return selectedBySheet;
    }
}
