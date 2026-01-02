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

public class AddFeatureAction extends AnAction {
    public AddFeatureAction() {
        super("➕ New Feature");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("AddFeatureAction.actionPerformed()");

        JTree tree = e.getData(CONTEXT_COMPONENT) instanceof JTree jTree ? jTree : null;
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof Directory treeItem) || treeItem.getType() == NodeType.FEATURE.getCode()) return;

        String name = Messages.showInputDialog("Enter feature name:", "Add Feature", null);
        if (name == null || name.isBlank()) return;

        Directory newFeature = new Directory().setType(NodeType.FEATURE.getCode()).setId(80).setName(name);
        newFeature.setFileName(newFeature.getType() + "_" + newFeature.getId() + "_" + newFeature.getName());
        newFeature.setFilePath(treeItem.getFilePath().resolve(newFeature.getFileName()));
        newFeature.setFile(new File(newFeature.getFileName()));

        try {
            Files.createDirectories(newFeature.getFilePath());
            System.out.println("Success! feature created: " + newFeature.getFilePath());
            refreshPath(newFeature.getFilePath());
        } catch (IOException ee) {
            System.err.println("Could not create feature: " + ee.getMessage());
        }

        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newFeature);
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        model.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
        tree.scrollPathToVisible(new TreePath(newNode.getPath()));
    }

}