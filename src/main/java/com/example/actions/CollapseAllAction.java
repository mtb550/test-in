package com.example.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;

public class CollapseAllAction extends AnAction {
    private final SimpleTree tree;


    public CollapseAllAction(final SimpleTree tree) {
        super("Collapse All", "Collapse all nodes", AllIcons.Actions.Collapseall);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        for (int i = tree.getRowCount() - 1; i >= 0; i--)
            tree.collapseRow(i);
    }
}
