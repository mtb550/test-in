package org.testin.editor.grid;

import com.intellij.openapi.Disposable;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

/**
 * The constructed grid view: the table, the scroll pane wrapping it, and the
 * disposable holding its font-sync subscription. The counterpart of
 * {@link org.testin.editor.list.ListView}.
 * <p>
 * One object because the three are built together, replaced together on every
 * rebuild, and gone together until the tester switches to the grid at all. As
 * three fields they were three separate questions an editor had to ask before
 * drawing anything, and three ways for the answers to disagree - a table without
 * the scroll pane that shows it (#66, finding 18).
 */
public record GridView(@NotNull JBTable table, @NotNull JBScrollPane scrollPane, @NotNull Disposable fontSync) {

    /**
     * Whether a cell is open for editing under the tester's cursor. A reload
     * would throw that half-typed value away, so an editor showing this grid
     * reports itself busy while it is true and an on-disk refresh waits for the
     * tester to finish (#20). One owner for the question, asked by both editors.
     */
    public boolean isCellOpen() {
        return table.isEditing();
    }

    /**
     * Commits what is being typed, before this view is thrown away.
     * <p>
     * A rebuild replaces the table, and an open editor goes with it - so a value
     * the tester had typed and not yet committed was simply gone, with nothing
     * said. {@link #isCellOpen} keeps an on-disk refresh from arriving mid-word
     * (#20), and every menu action is refused while a cell is open
     * ({@code NotWhileEditing}), but neither covers a rebuild that asks nobody:
     * a page turned under a background run, or a column ticked while the caret
     * sits in a cell.
     * <p>
     * Committing rather than cancelling, because that is what the tester's own
     * gesture already does - clicking away commits, through
     * {@code terminateEditOnFocusLost}. Losing the text was the outlier, not the
     * rule (#74).
     */
    public void commitOpenCell() {
        if (!isCellOpen()) return;

        table.getCellEditor().stopCellEditing();
    }
}
