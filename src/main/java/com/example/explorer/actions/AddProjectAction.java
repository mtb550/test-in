package com.example.explorer.actions;

import com.example.explorer.ProjectPanel;
import com.example.pojo.Config;
import com.example.pojo.Directory;
import com.example.util.NodeType;
import com.intellij.icons.AllIcons;
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

public class AddProjectAction extends AnAction {
    public final ProjectPanel projectPanel;
    public SimpleTree tree;

    public AddProjectAction(ProjectPanel projectPanel) {
        super("➕ New Project", "Add new project", AllIcons.General.Add);
        this.projectPanel = projectPanel;
        this.tree = projectPanel.getTestCaseTree();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("AddProjectAction.actionPerformed()");
        String name = Messages.showInputDialog("Enter project name:", "Add New Project", null);
        if (name == null || name.isBlank()) return;

        if (tree == null) {
            System.err.println("AddProjectAction.actionPerformed(): tree is null");
            return;
        }

        TreePath path = tree.getSelectionPath();
        DefaultMutableTreeNode parentNode;
        if (path == null) {
            System.out.println("AddProjectAction.actionPerformed(): path is null");
            parentNode = (DefaultMutableTreeNode) tree.getModel().getRoot();
        } else {
            parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        }

        Directory newProject = new Directory()
                .setType(NodeType.PROJECT.getCode())
                .setId(100)
                .setName(name)
                .setActive(1);

        newProject.setFileName(String.format("%d_%d_%s_%d", newProject.getType(), newProject.getId(), newProject.getName(), newProject.getActive()));
        newProject.setFilePath(Config.getRootFolder().toPath().resolve(newProject.getFileName()));
        newProject.setFile(new File(newProject.getFileName()));


        WriteAction.run(() -> {
            try {
                // 1. Get the parent directory as a VirtualFile
                File rootFolder = Config.getRootFolder();
                System.out.println(rootFolder.getPath());
                VirtualFile parentDir = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(rootFolder.toPath());
                System.out.println("parentDir: " + parentDir);

                if (parentDir != null) {
                    // 2. Create the directory using IntelliJ's API
                    // This triggers VFS events correctly and prevents the "watcher" clash
                    VirtualFile newDir = parentDir.createChildDirectory(this, newProject.getFileName());

                    projectPanel.getProjectSelector().addAndSelectProject(newProject);

                    // 3. Update your Tree Model immediately inside the same WriteAction
                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newProject);
                    DefaultTreeModel model = (DefaultTreeModel) projectPanel.getTestCaseTree().getModel();
                    model.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
                    tree.scrollPathToVisible(new TreePath(newNode.getPath()));

                    // 4. Ensure UI visibility
                    tree.scrollPathToVisible(new TreePath(newNode.getPath()));
                }
            } catch (IOException ex) {
                System.err.println("unable to create project: " + newProject);
            }
        });
    }
}