package org.testin.editor.listeners;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.model.RunEditorAttributes;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.RunStatusService;
import org.testin.services.Services;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Writes a run grid edit into the run (#74).
 * <p>
 * The counterpart of {@link GridEditListener}, and deliberately not the same
 * class: a test case edit writes a test case and regenerates automation code, a
 * run edit writes the run JSON and generates nothing. What the two do share -
 * the guards, and confirming the edit - is {@link AbstractGridEditListener}'s.
 */
public class RunGridEditListener extends AbstractGridEditListener {

    private final @NotNull RunEditor editor;

    /**
     * Repaints the list behind the grid, so a card shows what was typed into the
     * cell when the tester switches back.
     */
    private final @NotNull Runnable onEdited;

    public RunGridEditListener(final @NotNull Project p, final @NotNull RunEditor editor, final @NotNull List<TestCaseDto> pageItems, final @NotNull Runnable onEdited) {
        super(p, pageItems);
        this.editor = editor;
        this.onEdited = onEdited;
    }

    @Override
    protected int columnCount() {
        return RunEditorAttributes.values().length;
    }

    @Override
    protected boolean apply(final @NotNull DefaultTableModel model, final @NotNull TestCaseDto onThisRow, final int row, final int col) {
        final @NotNull RunEditorAttributes attr = RunEditorAttributes.values()[col];

        // The table model refuses these columns already; asked again of the same
        // attribute because a programmatic setValueAt never goes through the
        // model's answer.
        if (!attr.isEdited()) return false;

        final @NotNull Optional<TestRunItems> found = editor.runItem(onThisRow.getId());
        if (found.isEmpty()) return false;
        final @NotNull TestRunItems item = found.get();

        final @NotNull String before = attr.getRunValueExtractor().execute(item, p);

        // A run keeps what it recorded about a case that has since been deleted -
        // the same refusal the verdict path gives, worded once in the service.
        if (item.isRemoved()) {
            model.setValueAt(before, row, col);
            Services.getInstance(p, RunStatusService.class).refuseRemoved(p);
            return false;
        }

        attr.getRunValueSetter().execute(item, String.valueOf(model.getValueAt(row, col)));
        final @NotNull String after = attr.getRunValueExtractor().execute(item, p);

        // Written back whatever happened: the value the run now holds is what the
        // cell must show, even where the setter normalized what was typed.
        model.setValueAt(after, row, col);

        if (Objects.equals(before, after)) return false;

        Services.getInstance(p, RunStatusService.class).persistRun(p, editor);
        onEdited.run();

        // The details panel holds its own copy of the case, so typing into a
        // cell left it showing the previous value beside the cell just changed.
        // The test editor's dialog has always told it; the run editor's two
        // write paths never did.
        ViewToolWindowFactory.refreshIfShowing(p, List.of(onThisRow));

        return true;
    }
}
