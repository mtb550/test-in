package org.testin.clipboard;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.tree.TreeTransferHandler;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class CutNodeAction extends DumbAwareAction {

    private static final KeyStroke SHORTCUT = KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK);
    private final @NotNull SimpleTree tree;

    public CutNodeAction(final @NotNull SimpleTree tree) {
        super("Cut", "Cut selected items", AllIcons.Actions.MenuCut);
        this.tree = tree;
        this.registerCustomShortcutSet(Shortcuts.customShortcut(SHORTCUT), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (tree.getTransferHandler() instanceof TreeTransferHandler transferHandler) {
            transferHandler.copySelectionToClipboard(true);
        }
    }


    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - no update() here reads Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }

}
