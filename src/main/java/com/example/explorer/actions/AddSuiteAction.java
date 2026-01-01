package com.example.explorer.actions;

import com.example.pojo.Directory;
import com.example.util.NodeType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static com.example.util.Tools.refreshPath;
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

        if (!(userObject instanceof Directory treeItem) || treeItem.getType() == NodeType.FEATURE.getCode()) return;

        String name = Messages.showInputDialog("Enter suite name:", "Add Suite", null);
        if (name == null || name.isBlank()) return;

        Directory newSuite = new Directory().setType(NodeType.SUITE.getCode()).setId(50).setName(name);
        newSuite.setFileName(newSuite.getType() + "_" + newSuite.getId() + "_" + newSuite.getName());
        newSuite.setFilePath(treeItem.getFilePath().resolve(newSuite.getFileName()));
        newSuite.setFile(new File(newSuite.getFileName()));

        try {
            Files.createDirectories(newSuite.getFilePath());
            System.out.println("Success! suite created: " + newSuite.getFilePath());
            refreshPath(newSuite.getFilePath());
        } catch (IOException ee) {
            System.err.println("Could not create suite: " + ee.getMessage());
        }

        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newSuite);
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        model.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
        tree.scrollPathToVisible(new TreePath(newNode.getPath()));
    }
}