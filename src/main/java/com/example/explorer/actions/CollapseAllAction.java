package com.example.explorer.actions;

import com.example.explorer.ExplorerPanel;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CollapseAllAction extends AnAction {
    //private final ExplorerPanel panel;
    private final JTree tree;


    public CollapseAllAction(ExplorerPanel panel) {
        super("Collapse All", "Collapse all nodes", AllIcons.Actions.Collapseall);
        //this.panel = panel;
        this.tree = panel.getTestCaseTree();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        for (int i = tree.getRowCount() - 1; i >= 0; i--)
            tree.collapseRow(i);
    }
}
