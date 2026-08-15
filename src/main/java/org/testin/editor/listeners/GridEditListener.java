package org.testin.editor.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenType;
import org.testin.enums.TestEditorAttributes;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class GridEditListener implements TableModelListener {
    private final @NotNull Project p;
    private final @NotNull List<TestCaseDto> pageItems;
    private final @NotNull Runnable onEdited;
    /**
     * The test set this grid belongs to; the grid never mixes test sets.
     */
    private final @NotNull Path testSetPath;
    private boolean updating = false;

    public GridEditListener(final @NotNull Project p, final @NotNull List<TestCaseDto> pageItems,
                            final @NotNull Runnable onEdited, final @NotNull Path testSetPath) {
        this.p = p;
        this.pageItems = pageItems;
        this.onEdited = onEdited;
        this.testSetPath = testSetPath;
    }

    @Override
    public void tableChanged(final TableModelEvent e) {
        if (updating) return;
        if (e.getType() != TableModelEvent.UPDATE) return;
        final int row = e.getFirstRow();
        final int col = e.getColumn();
        if (row < 0 || col <= 0) return;
        if (!(e.getSource() instanceof DefaultTableModel model)
                || row >= model.getRowCount()
                || row >= pageItems.size()
                || col >= model.getColumnCount()
                || col - 1 >= TestEditorAttributes.values().length) return;

        updating = true;
        try {
            final TestEditorAttributes attr = TestEditorAttributes.values()[col - 1];
            final TestCaseDto tc = pageItems.get(row);

            final Object before = attr.gridValue(p, tc);
            attr.getImportSetter().execute(p, tc, String.valueOf(model.getValueAt(row, col)));
            final Object after = attr.gridValue(p, tc);

            // Always write the normalized value back to the cell - it renumbers
            // steps and drops blank entries even when nothing really changed.
            model.setValueAt(after, row, col);

            // Committing a cell without editing it must not rewrite the JSON or
            // regenerate code. Comparing the extracted values, not the raw text,
            // means a cosmetic difference alone is correctly treated as no change.
            if (Objects.equals(before, after)) return;

            persistAndGenerate(tc, attr);
            onEdited.run();
        } finally {
            updating = false;
        }
    }

    /**
     * Same behavior as the update dialog: write the test case JSON and update the
     * generated automation code for the edited attribute. Runs off the EDT — the
     * code generators schedule their own write command actions.
     */
    private void persistAndGenerate(final @NotNull TestCaseDto tc, final @NotNull TestEditorAttributes attr) {
        if (testSetPath.toString().isEmpty()) {
            Logger.warn("[grid] edit not persisted - the editor has no test set path");
            return;
        }

        final GenType generator = attr.getGenType();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Services.getInstance(p, ProjectIndexer.class).putTestCase(testSetPath, tc);
            generator.getAction().execute(p, tc);
        });
    }
}
