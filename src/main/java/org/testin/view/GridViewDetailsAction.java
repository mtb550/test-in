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
 * Enabled only on the order column, which is never editable; on every
 * other cell the action stays disabled so ENTER keeps starting an edit.
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
        e.getPresentation().setEnabled(!table.isEditing() && table.getSelectedRow() >= 0 && onSequence());
    }

    /**
     * Whether the tester is on the sequence number, and so whether ENTER opens
     * the details rather than starting an edit.
     * <p>
     * Written as a refusal first because that is the half a tester notices: an
     * editable cell answers no, whatever the column selection happens to look
     * like. Clicking a row number selects every column of that row so it copies
     * as one line, and after that the selection alone cannot say which cell the
     * tester meant - two attempts at reading it were both wrong. An editable
     * cell is never the sequence, so the question does not need to be read that
     * finely to answer it.
     * <p>
     * Then the two ways of being on the sequence: arrowed onto it, so the column
     * selection names it, or clicked on it, where the anchor is the only record
     * of where the click landed.
     */
    private boolean onSequence() {
        final int row = table.getSelectedRow();
        final int column = table.getSelectedColumn();

        // Asked first, and it settles the case that matters: a cell the tester
        // can type into is never the sequence, and ENTER there belongs to the
        // editor. Whatever the column selection looks like - and after a row
        // click it looks like several things at once - an editable cell means
        // this action stands down.
        if (row >= 0 && column >= 0 && table.isCellEditable(row, column)) return false;

        // Then either way of being on the sequence: arrowed onto it, so the
        // column selection names it; or clicked on it, which selects the whole
        // row and leaves the anchor behind as the only record of where.
        return GridPanelBuilder.isOrderColumn(table, column)
                || GridPanelBuilder.isOrderColumn(table, table.getColumnModel().getSelectionModel().getAnchorSelectionIndex());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // update() reads the table selection - EDT only (#52).
        return ActionUpdateThread.EDT;
    }
}
