package org.testin.editor.listeners;

import org.testin.notifications.Done;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.view.ViewToolWindowFactory;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.List;

/**
 * What both grids do when a tester types into a cell.
 * <p>
 * The test grid writes a test case and regenerates automation code; the run grid
 * writes the run. What is the same is everything around that, and it is the part
 * that is easy to get subtly wrong twice:
 * <p>
 * <b>Re-entrancy.</b> Writing the stored value back into the cell fires this
 * again, so a listener without the guard edits its own edit.
 * <p>
 * <b>No write when nothing changed.</b> A cell the tester tabbed through must not
 * rewrite the file and stamp it as modified. Compared on the extracted values
 * rather than the raw text, so a difference only the formatting can see is
 * correctly read as no change.
 * <p>
 * <b>Saying so.</b> Every state-changing action in Testin confirms itself with
 * one soft notification in the past tense, and typing into a grid cell was the
 * only one that did not - it wrote a test case to disk and said nothing, while
 * the dialog that changes the same field says "Updated". Said here, in the one
 * place, so the two grids cannot end up saying different words for one act
 * (#66, finding 29).
 */
public abstract class AbstractGridEditListener implements TableModelListener {

    protected final @NotNull Project p;

    private final @NotNull List<TestCaseDto> pageItems;

    /**
     * True while this listener is writing the stored value back into the cell it
     * was just handed.
     */
    private boolean updating = false;

    protected AbstractGridEditListener(final @NotNull Project p, final @NotNull List<TestCaseDto> pageItems) {
        this.p = p;
        this.pageItems = pageItems;
    }

    /**
     * Final, because everything it does is the part neither grid should be
     * writing for itself. What differs between them is {@link #apply}.
     */
    @Override
    public final void tableChanged(final TableModelEvent e) {
        if (updating) return;
        if (e.getType() != TableModelEvent.UPDATE) return;

        final int row = e.getFirstRow();
        final int col = e.getColumn();
        if (row < 0 || col < 0) return;
        if (!(e.getSource() instanceof DefaultTableModel model)
                || row >= model.getRowCount()
                || row >= pageItems.size()
                || col >= model.getColumnCount()
                || col >= columnCount()) return;

        final @NotNull TestCaseDto edited = pageItems.get(row);

        updating = true;
        try {
            if (!apply(model, edited, row, col)) return;

            confirmEdit();

            // Beside the confirmation, and for the same reason: the details panel
            // keeps its own copy of the case, so a cell edited under an open
            // panel left it showing the value that had just been replaced. Here
            // rather than in each subclass - both were writing the line, which is
            // how the run editor came to have it and the test editor not.
            ViewToolWindowFactory.refreshIfShowing(p, List.of(edited));
        } finally {
            updating = false;
        }
    }

    /**
     * How many columns this grid's attributes describe, so a cell beyond them is
     * refused before anything indexes the enum with it.
     */
    protected abstract int columnCount();

    /**
     * Writes one cell into whatever this grid is a view of, and answers whether
     * anything actually changed.
     * <p>
     * False for a cell that was committed unchanged, for a column that cannot be
     * edited, and for a row that refused the edit for its own reason - none of
     * those is something to confirm, because none of them happened.
     *
     * @param onThisRow the test case this row stands for, which both grids are a
     *                  view of even where what they write is not the case itself
     */
    protected abstract boolean apply(final @NotNull DefaultTableModel model, final @NotNull TestCaseDto onThisRow, final int row, final int col);

    /**
     * The word the update dialog already uses for the same act, because it is the
     * same act: an existing thing now says something different.
     */
    private void confirmEdit() {
        Services.getInstance(p, Notifier.class).softShow(p, Done.UPDATED);
    }
}
