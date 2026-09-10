package org.testin.clipboard;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.tree.TreeTransferHandler;

/**
 * UC-TREE-PANEL-013.
 * <p>
 * Declared in {@code plugin.xml} (#119), with no constructor and no fields for
 * the reason {@link CopyNodeAction} gives: the platform builds one instance for
 * the whole IDE, so the tree comes from the keystroke.
 * <p>
 * And with no default key, for the reason it gives too - CTRL+X is the grid's
 * gesture as well as the tree's, so it is put on this action by the tree rather
 * than claimed for the whole IDE from the keymap.
 */
public class CutNodeAction extends DumbAwareAction {

    // UC-TREE-PANEL-013
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        TreeTransferHandler.of(e).ifPresent(handler -> handler.copySelectionToClipboard(true));
    }

    /**
     * UC-TREE-PANEL-013, Rule-TREE-PANEL-002.
     * <p>
     * Greyed out where there is nothing to cut: a test project and the two
     * containers under it are the tree's fixed shape, not nodes that go
     * anywhere.
     * <p>
     * Greyed rather than hidden, so the menu keeps the same shape whatever is
     * right-clicked - a tester learns what a node cannot do by reading it, not
     * by noticing an entry that is missing.
     * <p>
     * And gray outside the Testin tree, where there is no handler to ask at all
     * (#119).
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TreeTransferHandler.of(e)
                .filter(TreeTransferHandler::hasTransferableSelection)
                .isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // update() reads the tree's selection, which is Swing state (#52).
        return ActionUpdateThread.EDT;
    }

}
