package com.example.explorer.actions;

import com.example.pojo.Directory;
import com.example.util.sql;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import static com.intellij.openapi.actionSystem.PlatformCoreDataKeys.CONTEXT_COMPONENT;

public class RenameAction extends AnAction {
    public RenameAction() {
        super("✏️ Rename");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        JTree tree = e.getData(CONTEXT_COMPONENT) instanceof JTree jTree ? jTree : null;
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
