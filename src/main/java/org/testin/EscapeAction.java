package org.testin;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.testEditor.TestEditorContextMenu;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.projectPanel.tree.TreeTransferHandler;
import org.testin.util.KeyboardSet;
import org.testin.util.logger.Logger;
import org.testin.viewPanel.ViewToolWindowFactory;

import java.awt.datatransfer.StringSelection;

public class EscapeAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final SimpleTree tree;
    private final TreeTransferHandler transferHandler;
    private final JBList<TestCaseDto> list;

    public EscapeAction(final @NotNull Project p, final @NotNull SimpleTree tree, final @NotNull TreeTransferHandler transferHandler) {
        super("Escape Action", "", AllIcons.Actions.InlayGear);
        this.p = p;
        this.tree = tree;
        this.transferHandler = transferHandler;
        this.list = null;
        this.registerCustomShortcutSet(KeyboardSet.Escape.getCustomShortcut(), tree);
    }

    public EscapeAction(final @NotNull Project p, final @NotNull JBList<TestCaseDto> list) {
        super("Escape Action", "", AllIcons.Actions.InlayGear);
        this.p = p;
        this.list = list;
        this.tree = null;
        this.transferHandler = null;
        this.registerCustomShortcutSet(KeyboardSet.Escape.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (tree != null && transferHandler != null) {
            transferHandler.getSelectedNodes().clear();
            transferHandler.resetLastAction();
            tree.repaint();
            Logger.info("Clipboard/Cut state cleared via ESC.");
            return;
        }

        if (list != null) {
            if (TestEditorContextMenu.isGlobalCutAction()) {
                TestEditorContextMenu.clearCutState();
            }

            CopyPasteManager.getInstance().setContents(new StringSelection(""));

            ToolWindow toolWindow = ViewToolWindowFactory.getToolWindow(p);

            if (toolWindow != null && toolWindow.isVisible()) {
                toolWindow.hide(null);
                return;
            }

            if (!list.isSelectionEmpty()) {
                list.clearSelection();
            }
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
