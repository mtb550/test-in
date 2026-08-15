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
import org.jetbrains.annotations.Nullable;
import org.testin.actions.AbstractProjectAction;
import org.testin.editor.testEditor.TestEditorContextMenu;
import org.testin.explorer.tree.TreeTransferHandler;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Shortcuts;
import org.testin.view.ViewToolWindowFactory;

import java.awt.datatransfer.StringSelection;

public class EscapeAction extends AbstractProjectAction {

    // Exactly one of tree / list / table is set, by the constructor that was
    // used; the surface this action was registered on is the one that is
    // non-null. transferHandler accompanies the tree.
    private final @Nullable SimpleTree tree;
    private final @Nullable TreeTransferHandler transferHandler;
    private final @Nullable JBList<TestCaseDto> list;
    private final @Nullable JBTable table;

    public EscapeAction(final @NotNull Project p, final @NotNull SimpleTree tree, final @NotNull TreeTransferHandler transferHandler) {
        super(p, "Escape Action", "", AllIcons.Actions.InlayGear);
        this.tree = tree;
        this.transferHandler = transferHandler;
        this.list = null;
        this.table = null;
        this.registerCustomShortcutSet(Shortcuts.Escape.getCustomShortcut(), tree);
    }

    public EscapeAction(final @NotNull Project p, final @NotNull JBList<TestCaseDto> list) {
        super(p, "Escape Action", "", AllIcons.Actions.InlayGear);
        this.list = list;
        this.table = null;
        this.tree = null;
        this.transferHandler = null;
        this.registerCustomShortcutSet(Shortcuts.Escape.getCustomShortcut(), list);
    }

    /**
     * Grid view: same behavior as the list, except while a cell is being edited.
     */
    public EscapeAction(final @NotNull Project p, final @NotNull JBTable table) {
        super(p, "Escape Action", "", AllIcons.Actions.InlayGear);
        this.table = table;
        this.list = null;
        this.tree = null;
        this.transferHandler = null;
        this.registerCustomShortcutSet(Shortcuts.Escape.getCustomShortcut(), table);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (tree != null && transferHandler != null) {
            transferHandler.getSelectedNodes().clear();
            transferHandler.resetLastAction();
            Logger.info("Clipboard/Cut state cleared via ESC.");
            return;
        }

        if (list != null) {
            clearClipboardState();
            if (hideViewPanelIfVisible()) return;

            if (!list.isSelectionEmpty()) {
                list.clearSelection();
            }
            return;
        }

        if (table != null) {
            // While a cell is open the editor owns ESC: cancel the edit only.
            if (table.isEditing()) {
                table.getCellEditor().cancelCellEditing();
                return;
            }

            clearClipboardState();
            if (hideViewPanelIfVisible()) return;

            if (table.getSelectedRowCount() > 0 || table.getSelectedColumnCount() > 0) {
                table.clearSelection();
            }
        }
    }

    private void clearClipboardState() {
        if (TestEditorContextMenu.isGlobalCutAction()) {
            TestEditorContextMenu.clearCutState();
        }
        CopyPasteManager.getInstance().setContents(new StringSelection(""));
    }

    /**
     * True when the view panel was open and has been hidden by this ESC.
     */
    private boolean hideViewPanelIfVisible() {
        final ToolWindow toolWindow = ViewToolWindowFactory.getToolWindow(p);
        if (toolWindow != null && toolWindow.isVisible()) {
            toolWindow.hide(null);
            return true;
        }
        return false;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - no update() here reads Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }

}
