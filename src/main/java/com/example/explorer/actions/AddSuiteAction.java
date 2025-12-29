package com.example.explorer.actions;

import com.example.pojo.Tree;
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

public class AddSuiteAction extends AnAction {
    public AddSuiteAction() {
        super("➕ New Suite");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        JTree tree = e.getData(CONTEXT_COMPONENT) instanceof JTree jTree ? jTree : null;
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof Tree treeItem) || treeItem.getType() == NodeType.FEATURE.getCode()) return;

        String name = Messages.showInputDialog("Enter suite name:", "Add Suite", null);
        if (name == null || name.isBlank()) return;

        sql db = new sql();
        int newNodeId = db.get("INSERT INTO tree (name, type, link, created_by) VALUES (?, ?, ?, ?) RETURNING id;",
                name, NodeType.SUITE.getCode(), treeItem.getId(), System.getProperty("user.name")).asType(Integer.class);

        // Build new node and insert it
        Tree newSuite = new Tree()
                .setType(NodeType.SUITE.getCode()).
                setLink(treeItem.getId())
                .setId(newNodeId);
        newSuite.setName(name);

        DefaultMutableTreeNode newSuiteNode = new DefaultMutableTreeNode(newSuite);
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        model.insertNodeInto(newSuiteNode, parentNode, parentNode.getChildCount());

        tree.scrollPathToVisible(new TreePath(newSuiteNode.getPath()));
    }
}
