package org.testin.clipboard;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.tree.TreeTransferHandler;
import org.testin.util.Shortcuts;

public class CopyNodeAction extends DumbAwareAction {
    private final @NotNull SimpleTree tree;

    public CopyNodeAction(final @NotNull SimpleTree tree) {
        super("Copy", "Copy selected items", AllIcons.Actions.Copy);
        this.tree = tree;
        this.registerCustomShortcutSet(Shortcuts.CopyItem.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (tree.getTransferHandler() instanceof TreeTransferHandler transferHandler) {
            transferHandler.copySelectionToClipboard(false);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - no update() here reads Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }

}
