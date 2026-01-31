package com.example.actions;

import com.example.pojo.Directory;
import com.example.util.NodeType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.IOException;


public class AddFeatureAction extends AnAction {
    private final SimpleTree tree;

    public AddFeatureAction(final SimpleTree tree) {
        super("➕ New Feature");
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("AddFeatureAction.actionPerformed()");
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof Directory treeItem) || treeItem.getType() == NodeType.FEATURE.getCode()) return;

        String name = Messages.showInputDialog("Enter feature name:", "Add Feature", null);
        if (name == null || name.isBlank()) return;

        Directory newFeature = new Directory()
                .setType(NodeType.FEATURE.getCode())
                .setId(80)
                .setName(name);

        newFeature.setFileName(newFeature.getType() + "_" + newFeature.getId() + "_" + newFeature.getName());
        newFeature.setFilePath(treeItem.getFilePath().resolve(newFeature.getFileName()));
        newFeature.setFile(new File(newFeature.getFileName()));

        WriteAction.run(() -> {
            try {
                // 1. Get the parent directory as a VirtualFile
                VirtualFile parentDir = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(treeItem.getFilePath());

                if (parentDir != null) {
                    // 2. Create the directory using IntelliJ's API
                    // This triggers VFS events correctly and prevents the "watcher" clash
                    VirtualFile newDir = parentDir.createChildDirectory(this, newFeature.getFileName());

                    // 3. Update your Tree Model immediately inside the same WriteAction
                    DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newFeature);
                    model.insertNodeInto(newNode, parentNode, parentNode.getChildCount());

                    // 4. Ensure UI visibility
                    tree.scrollPathToVisible(new TreePath(newNode.getPath()));
                }
            } catch (IOException ex) {
                System.err.println("unable to create suite: " + newFeature);
            }
        });
    }

}