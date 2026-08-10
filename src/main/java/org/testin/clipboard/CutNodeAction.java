package org.testin.clipboard;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.projectPanel.tree.TreeTransferHandler;
import org.testin.util.KeyboardSet;

public class CutNodeAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull SimpleTree tree;

    public CutNodeAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super("Cut", "Cut selected items", AllIcons.Actions.MenuCut);
        this.p = p;
        this.tree = tree;
        this.registerCustomShortcutSet(KeyboardSet.CutNode.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (tree.getTransferHandler() instanceof TreeTransferHandler transferHandler) {
            transferHandler.copySelectionToClipboard(true);
        }
    }


    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
