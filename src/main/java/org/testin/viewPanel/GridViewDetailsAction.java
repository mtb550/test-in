package org.testin.viewPanel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;
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
 * Enabled only on the sequence ("#") column, which is never editable; on every
 * other cell the action stays disabled so ENTER keeps starting an edit.
 */
public class GridViewDetailsAction extends DumbAwareAction {

    private final @NotNull Project p;
    private final @NotNull JBTable table;
    private final @NotNull List<TestCaseDto> pageItems;
    private final @NotNull ArrayList<String> path;

    public GridViewDetailsAction(final @NotNull Project p, final @NotNull JBTable table,
                                 final @NotNull List<TestCaseDto> pageItems, final @NotNull ArrayList<String> path) {
        super("View Details", "", AllIcons.Actions.PreviewDetails);
        this.p = p;
        this.table = table;
        this.pageItems = pageItems;
        this.path = path;
        this.registerCustomShortcutSet(Shortcuts.Enter.getCustomShortcut(), table);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        showDetails(table.getSelectedRow());
    }

    /** Opens the details view for a row; shared by the ENTER key and the double click. */
    public void showDetails(final int row) {
        if (row < 0 || row >= pageItems.size()) return;

        ViewToolWindowFactory.showPanel(p, List.of(pageItems.get(row)), path, ViewPanel::focusDetailsTab);
    }

    /**
     * Double click on the sequence column opens the details view too, so the
     * grid offers the same two gestures as the list.
     */
    public void installDoubleClick() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(e)) return;

                final int column = table.columnAtPoint(e.getPoint());
                if (column < 0 || table.convertColumnIndexToModel(column) != 0) return;

                showDetails(table.rowAtPoint(e.getPoint()));
                e.consume();
            }
        });
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final int column = table.getSelectedColumn();
        final boolean onSequenceColumn = column >= 0 && table.convertColumnIndexToModel(column) == 0;

        e.getPresentation().setEnabled(!table.isEditing() && onSequenceColumn && table.getSelectedRow() >= 0);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // update() reads the table selection - EDT only (#52).
        return ActionUpdateThread.EDT;
    }
}
