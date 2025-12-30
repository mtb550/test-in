package com.example.explorer.actions;

import com.example.pojo.Directory;
import com.example.util.NodeType;
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

public class AddFeatureAction extends AnAction {
    public AddFeatureAction() {
        super("➕ New Feature");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        JTree tree = e.getData(CONTEXT_COMPONENT) instanceof JTree jTree ? jTree : null;
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();
        if (!(userObject instanceof Directory treeItem) || treeItem.getType() == NodeType.FEATURE.getCode()) return;

        String name = Messages.showInputDialog("Enter feature name:", "Add Feature", null);
        if (name == null || name.isBlank()) return;

        sql db = new sql(); // TODO:: to be removed
        int newFeatureId = db.get("INSERT INTO tree (name, type, link, created_by) VALUES (?, ?, ?, ?) RETURNING id;",
                name, NodeType.FEATURE.getCode(), treeItem.getId(), System.getProperty("user.name")).asType(Integer.class);

        Directory newFeature = new Directory()
                .setType(NodeType.FEATURE.getCode()).
                setId(newFeatureId)
                .setLink(treeItem.getId());
        newFeature.setName(name);

        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newFeature);
        ((DefaultTreeModel) tree.getModel()).insertNodeInto(newNode, parentNode, parentNode.getChildCount());
        tree.scrollPathToVisible(new TreePath(newNode.getPath()));
    }

}
