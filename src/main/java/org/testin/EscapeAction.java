package org.testin;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.clipboard.CutState;
import org.testin.services.Services;
import org.testin.actions.AbstractProjectAction;
import org.testin.explorer.tree.TreeTransferHandler;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Shortcuts;
import org.testin.view.ViewToolWindowFactory;

import java.awt.datatransfer.StringSelection;

public class EscapeAction extends AbstractProjectAction {

    /**
     * What ESC does on the surface this action was registered on, chosen by the
     * constructor that was used.
     * <p>
     * It used to be four fields - a tree, its transfer handler, a list and a
     * table - of which exactly one set was filled in, and the action worked out
     * again at every press which constructor had been called (#71).
     */
    private final @NotNull Runnable onEscape;

    public EscapeAction(final @NotNull Project p, final @NotNull SimpleTree tree, final @NotNull TreeTransferHandler transferHandler) {
        super(p, "Escape Action", "Clear a pending cut or copy in the tree", AllIcons.Actions.InlayGear);
        this.onEscape = () -> clearTreeTransfer(transferHandler);
        this.registerCustomShortcutSet(Shortcuts.Escape.getCustomShortcut(), tree);
    }

    public EscapeAction(final @NotNull Project p, final @NotNull JBList<TestCaseDto> list) {
        super(p, "Escape Action", "Clear a pending cut or copy, close the details panel, then clear the selection", AllIcons.Actions.InlayGear);
        this.onEscape = () -> stepBack(list::clearSelection);
        this.registerCustomShortcutSet(Shortcuts.Escape.getCustomShortcut(), list);
    }

    /**
     * Grid view: same behavior as the list, except while a cell is being edited.
     */
    public EscapeAction(final @NotNull Project p, final @NotNull JBTable table) {
        super(p, "Escape Action", "Cancel the cell being edited, or clear the selection when not editing", AllIcons.Actions.InlayGear);
        this.onEscape = () -> escapeInGrid(table);
        this.registerCustomShortcutSet(Shortcuts.Escape.getCustomShortcut(), table);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        onEscape.run();
    }

    private void clearTreeTransfer(final @NotNull TreeTransferHandler transferHandler) {
        transferHandler.getSelectedNodes().clear();
        transferHandler.resetLastAction();
        Logger.info("Clipboard/Cut state cleared via ESC.");
    }

    private void escapeInGrid(final @NotNull JBTable table) {
        // While a cell is open the editor owns ESC: cancel the edit only.
        if (table.isEditing()) {
            table.getCellEditor().cancelCellEditing();
            return;
        }

        stepBack(table::clearSelection);
    }

    /**
     * One step back per press, on any surface that shows test cases: drop a
     * pending cut, then close the details panel, then clear the selection.
     * Clearing a selection that is already empty is what Swing does with it -
     * nothing - so it is not asked about first.
     */
    private void stepBack(final @NotNull Runnable clearSelection) {
        clearClipboardState();
        if (hideViewPanelIfVisible()) return;

        clearSelection.run();
    }

    private void clearClipboardState() {
        if (Services.getInstance(p, CutState.class).isCutting()) {
            Services.getInstance(p, CutState.class).clear();
        }
        CopyPasteManager.getInstance().setContents(new StringSelection(""));
    }

    /**
     * True when the view panel was open and has been hidden by this ESC.
     */
    private boolean hideViewPanelIfVisible() {
        return ViewToolWindowFactory.toolWindow(p)
                .filter(ToolWindow::isVisible)
                .map(toolWindow -> {
                    toolWindow.hide(null);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - no update() here reads Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }

}
