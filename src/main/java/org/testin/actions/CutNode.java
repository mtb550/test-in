package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.util.KeyboardSet;

import javax.swing.*;
import java.awt.event.ActionEvent;


public class CutNode extends DumbAwareAction {
    private final @NotNull SimpleTree tree;

    public CutNode(final @NotNull SimpleTree tree) {
        super("Cut", "Cut selected items", AllIcons.Actions.MenuCut);
        this.tree = tree;
        this.registerCustomShortcutSet(KeyboardSet.CutNode.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        Action action = tree.getActionMap().get("cut");
        if (action != null) {
            action.actionPerformed(new ActionEvent(tree, ActionEvent.ACTION_PERFORMED, "cut"));
        }
    }


    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
