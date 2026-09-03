package org.testin.editor.grid;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.Project;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.model.dto.TestCaseDto;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Everything ENTER does in a grid, in one place.
 * <p>
 * Three outcomes, decided by the cell the tester is on:
 * <ul>
 *   <li>a cell that can be typed into starts editing;</li>
 *   <li>the sequence number opens the details panel;</li>
 *   <li>anything else does nothing.</li>
 * </ul>
 * <p>
 * <b>One handler, on purpose.</b> This used to be two: an {@code InputMap}
 * binding that started the edit, and this action for the details, each with its
 * own idea of when it applied. They could not see each other, and the IDE
 * dispatches a registered action before a component's input map - so the split
 * was not a division of labour, it was a race that one side always won. It took
 * four attempts to fix, three of them spent on the half that was already
 * behaving. A key with one handler cannot have that bug.
 * <p>
 * It stays enabled for all three outcomes, including the one that does nothing,
 * so ENTER is answered here and never falls through to a second claimant. The one
 * time it stands down is while a cell editor is open, where ENTER belongs to the
 * editor and commits what was typed.
 *
 * @see GridKeys where the key is declared
 */
public final class GridEnterAction extends AbstractProjectAction {

    private final @NotNull JBTable table;
    private final @NotNull List<TestCaseDto> pageItems;
    private final @NotNull ArrayList<String> path;

    public GridEnterAction(final @NotNull Project p, final @NotNull JBTable table, final @NotNull List<TestCaseDto> pageItems, final @NotNull ArrayList<String> path) {
        super(p, "View Details", "Show the selected grid row in the details panel", AllIcons.Actions.PreviewDetails);
        this.table = table;
        this.pageItems = pageItems;
        this.path = path;
        this.registerCustomShortcutSet(new CustomShortcutSet(GridKeys.enter()), table);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final int row = table.getSelectedRow();
        if (row < 0) return;

        final int column = table.getSelectedColumn();

        if (column >= 0 && table.isCellEditable(row, column)) {
            startEditing(row, column);
            return;
        }

        if (onSequence(column)) showDetails(row);
    }

    /**
     * Opens the cell for typing and puts the caret in it, which is what makes it
     * feel like the double click it replaces.
     */
    private void startEditing(final int row, final int column) {
        if (!table.editCellAt(row, column)) return;

        Optional.ofNullable(table.getEditorComponent()).ifPresent(Component::requestFocusInWindow);
    }

    /**
     * Whether the tester is on the sequence number.
     * <p>
     * Two ways to be there, and both happen. Arrowing onto the column selects
     * that cell and names it. Clicking the number runs
     * {@link SequenceColumnRowSelector}, which selects every column so the row
     * copies as one line - and then the anchor is the only record of where the
     * click landed.
     */
    private boolean onSequence(final int column) {
        return GridPanelBuilder.isOrderColumn(table, column)
                || GridPanelBuilder.isOrderColumn(table, table.getColumnModel().getSelectionModel().getAnchorSelectionIndex());
    }

    /**
     * Opens the details view for a row; shared by ENTER and the double click.
     */
    public void showDetails(final int row) {
        if (row < 0 || row >= pageItems.size()) return;

        ViewToolWindowFactory.showPanel(p, List.of(pageItems.get(row)), path, ViewPanel::focusDetailsTab);
    }

    /**
     * Double-click on the sequence column opens the details too, so the grid
     * offers the same two gestures as the list. Every other column is left to the
     * table, where a double click starts an edit if the cell allows one.
     */
    public void installDoubleClick() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(e)) return;

                if (!GridPanelBuilder.isOrderColumn(table, table.columnAtPoint(e.getPoint()))) return;

                showDetails(table.rowAtPoint(e.getPoint()));
                e.consume();
            }
        });
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Enabled for all three outcomes, so no second handler ever sees ENTER.
        // Not while a cell is open: there the key commits what was typed, which
        // is the editor's own binding.
        e.getPresentation().setEnabled(!table.isEditing() && table.getSelectedRow() >= 0);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // update() reads the table selection - EDT only (#52).
        return ActionUpdateThread.EDT;
    }
}
