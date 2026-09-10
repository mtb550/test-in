package org.testin.clipboard;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.tree.TreeTransferHandler;

/**
 * UC-TREE-PANEL-014.
 * <p>
 * Declared in {@code plugin.xml} (#119), which is what puts it in Find Action
 * and lists it in Settings -> Keymap. That is why it has no constructor and no
 * fields: the platform builds one instance for the whole IDE, so the tree it
 * copies from comes from the keystroke rather than from whoever built it.
 * <p>
 * <b>And declared with no default key.</b> CTRL+C is the grid's key for its own
 * cells and the card list's for a test case, and a registered shortcut is
 * dispatched before a component's input map - so one keymap entry would answer
 * for all three and silently replace what the grid binds. The tree puts the key
 * on this action itself instead, which keeps one action behind the menu entry
 * and the key without taking the gesture away from anybody else.
 */
public class CopyNodeAction extends DumbAwareAction {

    // UC-TREE-PANEL-014
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        TreeTransferHandler.of(e).ifPresent(handler -> handler.copySelectionToClipboard(false));
    }

    /**
     * UC-TREE-PANEL-014, Rule-TREE-PANEL-002.
     * <p>
     * Greyed out where there is nothing to copy: a test project and the two
     * containers under it are the tree's fixed shape, not nodes that go
     * anywhere.
     * <p>
     * Greyed rather than hidden, so the menu keeps the same shape whatever is
     * right-clicked - a tester learns what a node cannot do by reading it, not
     * by noticing an entry that is missing.
     * <p>
     * Now also the guard that keeps the key to itself: there is no handler to
     * ask when the keystroke arrived outside the Testin tree, so this is gray in
     * a Java file rather than copying whatever a tree behind it holds (#119).
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
