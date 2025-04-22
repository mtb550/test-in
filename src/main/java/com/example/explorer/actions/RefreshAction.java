package com.example.explorer.actions;

import com.example.explorer.ExplorerPanel;
import com.example.explorer.ExplorerTree;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;

public class RefreshAction extends AnAction {
    private final ExplorerPanel panel;
    private final SimpleTree tree;

    public RefreshAction(ExplorerPanel panel) {
        super("Refresh", "Reload tree", AllIcons.Actions.Refresh);
        this.panel = panel;
        this.tree = this.panel.getProjectTree();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        //ExplorerTree.build();
        //tree.setModel(ExplorerTree.getTreeModel());
        ExplorerTree.getTreeModel().reload(); //TODO:: try this may it is better than code above
        panel.loadAllProjects();
    }
}
