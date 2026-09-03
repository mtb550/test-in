package org.testin.view;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.editor.grid.GridPanelBuilder;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * ENTER opens the details view for the selected grid row - the grid counterpart
 * of {@link ViewDetailsAction}, which does the same on the list. Registered as
 * an IDE action rather than a Swing key binding, because the IDE dispatcher
 * handles registered shortcuts before a component's own ActionMap.
 * <p>
 * ENTER means one thing in a grid: edit this cell, or show me what I cannot
 * edit. So this is enabled on every cell that refuses editing - the order
 * column among them, since it is never editable - and disabled on the ones that
 * accept it, where ENTER falls through to the table's own binding and starts the
 * edit. A read-only cell used to do nothing at all unless it was the order
 * column.
 */
public class GridViewDetailsAction extends AbstractProjectAction {
    private final @NotNull JBTable table;
    private final @NotNull List<TestCaseDto> pageItems;
    private final @NotNull ArrayList<String> path;

    public GridViewDetailsAction(final @NotNull Project p, final @NotNull JBTable table, final @NotNull List<TestCaseDto> pageItems, final @NotNull ArrayList<String> path) {
        super(p, "View Details", "Show the selected grid row in the details panel", AllIcons.Actions.PreviewDetails);
        this.table = table;
        this.pageItems = pageItems;
        this.path = path;
        this.registerCustomShortcutSet(Shortcuts.Enter.getCustomShortcut(), table);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        showDetails(table.getSelectedRow());
    }

    /**
     * Opens the details view for a row; shared by the ENTER key and the double click.
     */
    public void showDetails(final int row) {
        if (row < 0 || row >= pageItems.size()) return;

        ViewToolWindowFactory.showPanel(p, List.of(pageItems.get(row)), path, ViewPanel::focusDetailsTab);
    }

    /**
     * Double-click on the order column opens the details view too, so the
     * grid offers the same two gestures as the list.
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
        final int row = table.getSelectedRow();

        if (row < 0 || table.isEditing()) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // Every cell the tester cannot type into. An editable one leaves this
        // disabled, and ENTER falls through to the table's own binding, which
        // starts the edit - so one key means "edit this" where that is possible
        // and "show me this" where it is not.
        //
        // No column selected is the row-selection the order column produces, and
        // a row is exactly what the details show.
        final int column = table.getSelectedColumn();

        e.getPresentation().setEnabled(column < 0 || !table.isCellEditable(row, column));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // update() reads the table selection - EDT only (#52).
        return ActionUpdateThread.EDT;
    }
}
