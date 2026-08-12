package org.testin.editorPanel.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestEditorAttributes;
import org.testin.codegen.GeneratorType;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.Services;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.nio.file.Path;
import java.util.List;

public class GridEditListener implements TableModelListener {
    private final @NotNull Project p;
    private final List<TestCaseDto> pageItems;
    private final Runnable onEdited;
    private boolean updating = false;

    public GridEditListener(final @NotNull Project p, final List<TestCaseDto> pageItems, final Runnable onEdited) {
        this.p = p;
        this.pageItems = pageItems;
        this.onEdited = onEdited;
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
            attr.getImportSetter().execute(p, tc, String.valueOf(model.getValueAt(row, col)));
            model.setValueAt(attr.getTestValueExtractor().execute(tc, p), row, col);

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
        final Path testSetPath = tc.getParent().getPath();
        if (testSetPath.toString().isEmpty()) return;

        final GeneratorType generator = attr.getGeneratorType();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Services.getInstance(p, ProjectIndexer.class).putTestCase(testSetPath, tc);
            if (generator != null) {
                generator.getAction().execute(p, tc);
            }
        });
    }
}
