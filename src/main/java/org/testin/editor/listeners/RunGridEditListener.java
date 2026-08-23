package org.testin.editor.listeners;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.model.RunEditorAttributes;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.RunStatusService;
import org.testin.services.Services;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Writes a run grid edit into the run (#74).
 * <p>
 * The counterpart of {@link GridEditListener}, which does the same for the test
 * grid, and deliberately not the same class: a test case edit writes a test case
 * and regenerates automation code, a run edit writes the run JSON and generates
 * nothing. Sharing one listener between them would mean a listener that asks
 * which editor it is in on every keystroke.
 * <p>
 * Both guards the test one carries are here for the same reasons. Re-entrancy,
 * because writing the normalized value back into the cell fires this again; and
 * no write when nothing changed, so a cell the tester tabbed through does not
 * rewrite the run and stamp it as modified.
 */
public class RunGridEditListener implements TableModelListener {

    private final @NotNull Project p;
    private final @NotNull RunEditor editor;
    private final @NotNull List<TestCaseDto> pageItems;

    /**
     * Repaints the list behind the grid, so a card shows what was typed into the
     * cell when the tester switches back.
     */
    private final @NotNull Runnable onEdited;

    private boolean updating = false;

    public RunGridEditListener(final @NotNull Project p, final @NotNull RunEditor editor,
                               final @NotNull List<TestCaseDto> pageItems, final @NotNull Runnable onEdited) {
        this.p = p;
        this.editor = editor;
        this.pageItems = pageItems;
        this.onEdited = onEdited;
    }

    @Override
    public void tableChanged(final TableModelEvent e) {
        if (updating) return;
        if (e.getType() != TableModelEvent.UPDATE) return;

        final int row = e.getFirstRow();
        final int col = e.getColumn();
        if (row < 0 || col < 0) return;
        if (!(e.getSource() instanceof DefaultTableModel model)
                || row >= model.getRowCount()
                || row >= pageItems.size()
                || col >= model.getColumnCount()
                || col >= RunEditorAttributes.values().length) return;

        updating = true;
        try {
            apply(model, RunEditorAttributes.values()[col], row, col);
        } finally {
            updating = false;
        }
    }

    private void apply(final @NotNull DefaultTableModel model, final @NotNull RunEditorAttributes attr,
                       final int row, final int col) {
        // The table model refuses these columns already; asked again of the same
        // attribute because a programmatic setValueAt never goes through the
        // model's answer.
        if (!attr.isEdited()) return;

        final @NotNull Optional<TestRunItems> found = editor.runItem(pageItems.get(row).getId());
        if (found.isEmpty()) return;
        final @NotNull TestRunItems item = found.get();

        final @NotNull String before = attr.getRunValueExtractor().execute(item, p);

        // A run keeps what it recorded about a case that has since been deleted -
        // the same refusal the verdict path gives, worded once in the service.
        if (item.isRemoved()) {
            model.setValueAt(before, row, col);
            Services.getInstance(p, RunStatusService.class).refuseRemoved(p);
            return;
        }

        attr.getRunValueSetter().execute(item, String.valueOf(model.getValueAt(row, col)));
        final @NotNull String after = attr.getRunValueExtractor().execute(item, p);

        // Written back whatever happened: the value the run now holds is what the
        // cell must show, even where the setter normalized what was typed.
        model.setValueAt(after, row, col);

        if (Objects.equals(before, after)) return;

        Services.getInstance(p, RunStatusService.class).persistRun(p, editor);
        onEdited.run();
    }
}
