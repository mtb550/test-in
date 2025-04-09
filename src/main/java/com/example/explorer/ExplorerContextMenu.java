package com.example.explorer;

import com.example.pojo.Tree;
import com.example.util.sql;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.intellij.openapi.actionSystem.PlatformCoreDataKeys.CONTEXT_COMPONENT;

public class ExplorerContextMenu extends DefaultActionGroup {

    public ExplorerContextMenu() {
        super("Test Explorer Context Menu", true);

        DefaultActionGroup addGroup = new DefaultActionGroup("➕ Add", true);
        addGroup.add(new AddProjectAction());
        addGroup.add(new AddSuiteAction());
        addGroup.add(new AddFeatureAction());

        add(addGroup); // instead of adding the three separately
        addSeparator();
        add(new DeleteNodeAction());
        add(new RenameNodeAction());
        addSeparator();
        add(new RunFeatureAction());
        addSeparator();

        DefaultActionGroup exportGroup = new DefaultActionGroup("📥 Export", true);
        exportGroup.add(new ExportCsvAction());
        exportGroup.add(new ExportHtmlAction());
        exportGroup.add(new ExportExcelAction());
        exportGroup.add(new ExportJsonAction());
        add(exportGroup);

        add(new ImportAction());
        addSeparator();
        add(new OpenOldVersionsAction());
        add(new ViewCommitsAction());
    }

    public static class ViewCommitsAction extends AnAction {
        public ViewCommitsAction() {
            super("📌 View Pending Commits");
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            // TODO: Open commit log UI
        }
    }

    public static class OpenOldVersionsAction extends AnAction {
        public OpenOldVersionsAction() {
            super("🕓 Open Old Versions");
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            // TODO: Load old test case versions
        }
    }

    public static class ImportAction extends AnAction {
        public ImportAction() {
            super("📥 Import");
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            // TODO: Import test cases
        }
    }

    public static class ExportCsvAction extends AnAction {
        public ExportCsvAction() {
            super("Export as CSV");
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            // TODO: Export test cases to CSV
        }
    }

    public static class RunFeatureAction extends AnAction {
        public RunFeatureAction() {
            super("▶ Run Feature");
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            // TODO: Run the feature test automation
        }
    }

    public static class RenameNodeAction extends AnAction {
        public RenameNodeAction() {
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
            if (!(userObject instanceof TestCaseExplorerPanel.NodeInfo info)) return;

            String newName = Messages.showInputDialog("Rename node:", "Rename", null, info.name, null);
            if (newName == null || newName.isBlank()) return;

            sql db = new sql();
            try {
                db.execute("UPDATE tree SET name = ? WHERE id = ?", newName, info.id);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }

            info.name = newName;
            ((DefaultTreeModel) tree.getModel()).nodeChanged(node);
        }
    }


    public static class DeleteNodeAction extends AnAction {
        public DeleteNodeAction() {
            super("❌ Delete");
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            JTree tree = e.getData(CONTEXT_COMPONENT) instanceof JTree jTree ? jTree : null;
            if (tree == null) return;

            TreePath path = tree.getSelectionPath();
            if (path == null) return;

            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (!(selectedNode.getUserObject() instanceof TestCaseExplorerPanel.NodeInfo nodeInfo)) return;

            int confirm = Messages.showYesNoDialog("Delete '" + nodeInfo.name + "' and all its children?", "Confirm Recursive Delete", null);
            if (confirm != Messages.YES) return;

            sql db = new sql();

            try {
                // Collect all descendant IDs including this one
                List<Integer> idsToDelete = new ArrayList<>();
                collectIdsRecursively(nodeInfo.id, db, idsToDelete);

                // Delete from DB
                String inClause = idsToDelete.stream().map(id -> "?").collect(Collectors.joining(","));
                db.execute("DELETE FROM tree WHERE id IN (" + inClause + ")", idsToDelete.toArray());

                // Remove from UI
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                model.removeNodeFromParent(selectedNode);

            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }

        private void collectIdsRecursively(int id, sql db, List<Integer> out) throws SQLException {
            out.add(id);
            Tree[] children = db.get("SELECT * FROM tree WHERE link = ?", id).as(Tree[].class);
            for (Tree child : children) {
                collectIdsRecursively(child.getId(), db, out);
            }
        }
    }


    public static class AddFeatureAction extends AnAction {
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
            if (!(userObject instanceof TestCaseExplorerPanel.NodeInfo parentInfo) || parentInfo.type == 2) return;

            String name = Messages.showInputDialog("Enter feature name:", "Add Feature", null);
            if (name == null || name.isBlank()) return;

            sql db = new sql();
            try {
                db.execute("INSERT INTO tree (name, type, link, created_by, created_at) VALUES (?, ?, ?, ?, datetime('now'))",
                        name, 2, parentInfo.id, System.getProperty("user.name"));
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }

            int newId = db.get("SELECT id from tree where name = ?", name).as(Tree.class).getId();
            TestCaseExplorerPanel.NodeInfo newFeature = new TestCaseExplorerPanel.NodeInfo(name, 2, newId);
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newFeature);
            ((DefaultTreeModel) tree.getModel()).insertNodeInto(newNode, parentNode, parentNode.getChildCount());
            tree.scrollPathToVisible(new TreePath(newNode.getPath()));
        }
    }


    public static class AddSuiteAction extends AnAction {
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

            if (!(userObject instanceof TestCaseExplorerPanel.NodeInfo parentInfo) || parentInfo.type == 2) return;

            String name = Messages.showInputDialog("Enter suite name:", "Add Suite", null);
            if (name == null || name.isBlank()) return;

            sql db = new sql();
            try {
                db.execute("INSERT INTO tree (name, type, link, created_by, created_at) VALUES (?, ?, ?, ?, datetime('now'))",
                        name, 1,parentInfo.id, System.getProperty("user.name"));
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }

            // Fetch new ID
            int newId = db.get("SELECT id from tree where name = ?",name).as(Tree.class).getId();

            // Build new node and insert it
            TestCaseExplorerPanel.NodeInfo newSuiteInfo = new TestCaseExplorerPanel.NodeInfo(name, 1, parentInfo.id);
            newSuiteInfo.id = newId;

            DefaultMutableTreeNode newSuiteNode = new DefaultMutableTreeNode(newSuiteInfo);
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.insertNodeInto(newSuiteNode, parentNode, parentNode.getChildCount());

            tree.scrollPathToVisible(new TreePath(newSuiteNode.getPath()));
        }
    }


    public static class ExportHtmlAction extends AnAction {
        public ExportHtmlAction() {
            super("Export as HTML");
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            // TODO: Implement export logic to HTML
        }
    }

    public static class ExportExcelAction extends AnAction {
        public ExportExcelAction() {
            super("Export as Excel");
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            // TODO: Implement export logic to EXCEL
        }
    }

    public static class ExportJsonAction extends AnAction {
        public ExportJsonAction() {
            super("Export as Json");
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            // TODO: Implement export logic to EXCEL
        }
    }

    public static class AddProjectAction extends AnAction {
        public AddProjectAction() {
            super("➕ New Project");
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            JTree tree = e.getData(CONTEXT_COMPONENT) instanceof JTree jTree ? jTree : null;
            if (tree == null) return;

            String name = Messages.showInputDialog("Enter project name:", "Add Project", null);
            if (name == null || name.isBlank()) return;

            sql db = new sql();
            try {
                db.execute("INSERT INTO tree (name, type, created_by, created_at) VALUES (?, ?, ?, datetime('now'))",
                        name, 0, System.getProperty("user.name"));
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }

            int newId = db.get("SELECT id FROM tree WHERE name = ?", name).as(Tree.class).getId();
            TestCaseExplorerPanel.NodeInfo newProject = new TestCaseExplorerPanel.NodeInfo(name, 0, newId);
            DefaultMutableTreeNode newProjectNode = new DefaultMutableTreeNode(newProject);

            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();

            model.insertNodeInto(newProjectNode, root, root.getChildCount());
            tree.scrollPathToVisible(new TreePath(newProjectNode.getPath()));
        }
    }



}
