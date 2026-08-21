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

    /**
     * Greyed out where there is nothing to copy: a test project and the two
     * containers under it are the tree's fixed shape, not nodes that go
     * anywhere.
     * <p>
     * Greyed rather than hidden, so the menu keeps the same shape whatever is
     * right-clicked - a tester learns what a node cannot do by reading it, not
     * by noticing an entry that is missing.
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(tree.getTransferHandler() instanceof TreeTransferHandler handler
                && handler.hasTransferableSelection());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // update() reads the tree's selection, which is Swing state (#52).
        return ActionUpdateThread.EDT;
    }

}
