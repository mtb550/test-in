package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.EditorCM;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.projectPanel.tree.TreeTransferHandler;
import org.testin.util.KeyboardSet;
import org.testin.util.logger.Log;
import org.testin.viewPanel.ViewToolWindowFactory;

import java.awt.datatransfer.StringSelection;

public class Escape extends DumbAwareAction {
    private final SimpleTree tree;
    private final TreeTransferHandler transferHandler;
    private final JBList<TestCaseDto> list;

    public Escape(final SimpleTree tree, final TreeTransferHandler transferHandler) {
        super("Escape Action", "", AllIcons.Actions.InlayGear);
        this.tree = tree;
        this.transferHandler = transferHandler;
        this.list = null;
        this.registerCustomShortcutSet(KeyboardSet.Escape.getCustomShortcut(), tree);
    }

    public Escape(final JBList<TestCaseDto> list) {
        super("Escape Action", "", AllIcons.Actions.InlayGear);
        this.list = list;
        this.tree = null;
        this.transferHandler = null;
        this.registerCustomShortcutSet(KeyboardSet.Escape.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (tree != null && transferHandler != null) {
            transferHandler.getSelectedNodes().clear();
            transferHandler.resetLastAction();
            tree.repaint();
            Log.info("Clipboard/Cut state cleared via ESC.");
            return;
        }

        if (list != null) {
            if (EditorCM.isGlobalCutAction()) {
                EditorCM.clearCutState();
            }

            CopyPasteManager.getInstance().setContents(new StringSelection(""));

            ToolWindow toolWindow = ViewToolWindowFactory.getToolWindow(e.getProject());

            if (toolWindow != null && toolWindow.isVisible()) {
                toolWindow.hide(null);
                return;
            }

            if (!list.isSelectionEmpty()) {
                list.clearSelection();
            }
        }
    }
}