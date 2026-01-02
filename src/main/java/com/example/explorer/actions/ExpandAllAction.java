package com.example.explorer.actions;

import com.example.explorer.ExplorerPanel;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ExpandAllAction extends AnAction {
    //private final ExplorerPanel panel;
    private final JTree tree;

    public ExpandAllAction(ExplorerPanel panel) {
        super("Expand All", "Expand all nodes", AllIcons.Actions.Expandall);
        //this.panel = panel;
        this.tree = panel.getTestCaseTree();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        for (int i = 0; i < tree.getRowCount(); i++)
            tree.expandRow(i);
    }
}
