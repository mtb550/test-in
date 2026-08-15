package org.testin.importexport.shared;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.ui.framework.DialogComponent;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The sheets about to be imported or exported: one tab each, a checkbox per
 * test case, and the editable cells the tester can correct before committing.
 * <p>
 * It keeps its table models, which is the whole point - a preview that cannot
 * be read back is decorative, and the export dialog was exactly that for as
 * long as it built its tabs and dropped them.
 */
public final class SheetPreview implements DialogComponent {

    private final @NotNull Project p;
    private final @NotNull List<TestEditorAttributes> attributes;

    private final @NotNull JBTabbedPane tabs = new JBTabbedPane();
    private final @NotNull Map<String, DefaultTableModel> models = new LinkedHashMap<>();

    private @NotNull Map<String, List<TestCaseDto>> sheets = new LinkedHashMap<>();

    public SheetPreview(final @NotNull Project p, final @NotNull List<TestEditorAttributes> attributes) {
        this.p = p;
        this.attributes = attributes;
    }

    /**
     * Replaces the preview with these sheets. The models go with the tabs:
     * keeping the previous file's would leave rows nobody can see still
     * selectable.
     */
    public void show(final @NotNull Map<String, List<TestCaseDto>> newSheets) {
        sheets = newSheets;

        models.clear();
        while (tabs.getTabCount() > 0) {
            tabs.removeTabAt(0);
        }

        for (final Map.Entry<String, List<TestCaseDto>> entry : sheets.entrySet()) {
            final List<TestCaseDto> testCases = entry.getValue();

            final DefaultTableModel model = new TablePanelBuilder().createModel(p, attributes, testCases);
            // Without this an edited cell is shown and then dropped: the model
            // holds it, and nothing carries it back to the test case.
            model.addTableModelListener(new CellEditListener(attributes, p, testCases));

            models.put(entry.getKey(), model);
            tabs.addTab(entry.getKey(), new JBScrollPane(new TablePanelBuilder().buildTable(model, p)));
        }
    }

    /**
     * True before anything has been loaded - the import dialog opens this way
     * and stays so until a file is chosen.
     */
    public boolean isEmpty() {
        return sheets.isEmpty();
    }

    /**
     * The ticked cases per sheet. A sheet with nothing ticked is left out, so an
     * empty result means the tester selected nothing at all.
     */
    public @NotNull Map<String, List<TestCaseDto>> selected() {
        final Map<String, List<TestCaseDto>> selectedBySheet = new LinkedHashMap<>();

        for (final Map.Entry<String, List<TestCaseDto>> entry : sheets.entrySet()) {
            final DefaultTableModel model = models.get(entry.getKey());
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

    @Override
    public @NotNull JComponent getPanel() {
        return tabs;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return tabs;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
        // Nothing here submits; the dialog's button does.
    }

    /**
     * The form above keeps the initial focus - the tester types a destination
     * before they touch the preview.
     */
    @Override
    public boolean wantsFocus() {
        return false;
    }

    @Override
    public boolean fillsSpace() {
        return true;
    }
}
