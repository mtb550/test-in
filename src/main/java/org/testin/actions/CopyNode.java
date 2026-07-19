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


public class CopyNode extends DumbAwareAction {
    private final @NotNull SimpleTree tree;

    public CopyNode(final @NotNull SimpleTree tree) {
        super("Copy", "Copy selected items", AllIcons.Actions.Copy);
        this.tree = tree;
        this.registerCustomShortcutSet(KeyboardSet.CopyNode.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        Action action = tree.getActionMap().get("copy");

        if (action != null)
            action.actionPerformed(new ActionEvent(tree, ActionEvent.ACTION_PERFORMED, "copy"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
