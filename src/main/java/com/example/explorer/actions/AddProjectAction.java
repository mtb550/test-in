package com.example.explorer.actions;

import com.example.explorer.ExplorerPanel;
import com.example.pojo.Tree;
import com.example.util.NodeType;
import com.example.util.sql;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class AddProjectAction extends AnAction {
    private final ExplorerPanel panel;
    private final SimpleTree tree;

    public AddProjectAction(ExplorerPanel panel) {
        super("New Project", "Add new project", AllIcons.General.Add);
        this.panel = panel;
        this.tree = this.panel.getProjectTree();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String name = Messages.showInputDialog("Enter project name:", "Add New Project", null);
        if (name == null || name.isBlank()) return;

        int newProjectId = new sql().get(
                "INSERT INTO tree (name, type, created_by) VALUES (?, ?, ?) RETURNING id;",
                name, NodeType.PROJECT.getCode(), System.getProperty("user.name")
        ).asType(Integer.class);

        Tree newProject = new Tree()
                .setType(NodeType.PROJECT.getCode())
                .setId(newProjectId);
        newProject.setName(name);

        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newProject);

        model.insertNodeInto(newNode, root, root.getChildCount());

        TreePath path = new TreePath(newNode.getPath());
        tree.scrollPathToVisible(path);
        tree.setSelectionPath(path);
    }
}
