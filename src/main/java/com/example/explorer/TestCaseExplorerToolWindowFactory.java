package com.example.explorer;

import com.example.pojo.Tree;
import com.example.util.sql;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.List;

public class TestCaseExplorerToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        TestCaseExplorerPanel explorerPanel = new TestCaseExplorerPanel();

        Content content = ContentFactory.getInstance()
                .createContent(explorerPanel.getPanel(), "", false);
        toolWindow.getContentManager().addContent(content);

        toolWindow.setTitleActions(List.of(createToolbarActions(explorerPanel).getChildren(null)));
    }

    private DefaultActionGroup createToolbarActions(TestCaseExplorerPanel explorerPanel) {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(new AnAction("Expand All", "Expand all nodes", AllIcons.Actions.Expandall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                explorerPanel.expandAll();
            }
        });

        group.add(new AnAction("Collapse All", "Collapse all nodes", AllIcons.Actions.Collapseall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                explorerPanel.collapseAll();
            }
        });

        group.addSeparator();

        group.add(new AnAction("Refresh", "Reload tree", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                explorerPanel.refresh();
            }
        });

        group.add(new AnAction("Settings", "Configure tree", AllIcons.General.Settings) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Messages.showInfoMessage("Settings coming soon!", "Info");
            }
        });

        group.add(createAddProjectAction(explorerPanel)); // TODO:: make all separated like this

        return group;
    }

    private AnAction createAddProjectAction(TestCaseExplorerPanel explorerPanel) {
        return new AnAction("New Project", "Add new project", AllIcons.General.Add) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                String name = Messages.showInputDialog("Enter project name:", "Add New Project", null);
                if (name == null || name.isBlank()) return;

                int newProjectId = new sql().get(
                        "INSERT INTO tree (name, type, created_by) VALUES (?, ?, ?) RETURNING id;",
                        name, 0, System.getProperty("user.name")
                ).asType(Integer.class);

                Tree newProject = new Tree()
                        .setName(name)
                        .setType(0)
                        .setId(newProjectId);

                JTree tree = explorerPanel.getTree();
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newProject);

                model.insertNodeInto(newNode, root, root.getChildCount());

                TreePath path = new TreePath(newNode.getPath());
                tree.scrollPathToVisible(path);
                tree.setSelectionPath(path);
            }
        };
    }
}
