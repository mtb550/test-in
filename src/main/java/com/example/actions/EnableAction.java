package com.example.actions;

import com.example.pojo.Directory;
import com.example.util.sql;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;


public class EnableAction extends AnAction {
    private final SimpleTree tree;

    public EnableAction(final SimpleTree tree) {
        super("✏️ Rename");
        this.tree = tree;
    }

    /// To Be updated. Enable Action for projects - Explorer, Test Cases
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();
        if (!(userObject instanceof Directory treeItem)) return;

        String newName = Messages.showInputDialog("Rename node:", "Rename", null, treeItem.getName(), null);
        if (newName == null || newName.isBlank()) return;

        new sql().execute("UPDATE tree SET name = ? WHERE id = ?", newName, treeItem.getId());


        treeItem.setName(newName);
        ((DefaultTreeModel) tree.getModel()).nodeChanged(node);
    }
}
