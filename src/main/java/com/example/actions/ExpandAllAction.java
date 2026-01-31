package com.example.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;

public class ExpandAllAction extends AnAction {
    private final SimpleTree tree;

    public ExpandAllAction(SimpleTree tree) {
        super("Expand All", "Expand all nodes", AllIcons.Actions.Expandall);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        for (int i = 0; i < tree.getRowCount(); i++)
            tree.expandRow(i);
    }
}
