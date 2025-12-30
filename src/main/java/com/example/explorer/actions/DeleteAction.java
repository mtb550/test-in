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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.intellij.openapi.actionSystem.PlatformCoreDataKeys.CONTEXT_COMPONENT;

public class DeleteAction extends AnAction {
    public DeleteAction() {
        super("❌ Delete");
    }

    public void actionPerformed(@NotNull AnActionEvent e) {
        JTree tree = e.getData(CONTEXT_COMPONENT) instanceof JTree jTree ? jTree : null;
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (!(selectedNode.getUserObject() instanceof Directory treeItem)) return;

        int confirm = Messages.showYesNoDialog("Delete '" + treeItem.getName() + "' and all its children?", "Confirm Recursive Delete", null);
        if (confirm != Messages.YES) return;

        sql db = new sql();

        try {
            // Collect all descendant IDs including this one
            List<Integer> idsToDelete = new ArrayList<>();
            collectIdsRecursively(treeItem.getId(), db, idsToDelete);

            // Delete from DB
            String inClause = idsToDelete.stream().map(id -> "?").collect(Collectors.joining(","));
            db.execute("DELETE FROM nafath_tc_tree WHERE id IN (" + inClause + ")", idsToDelete.toArray());

            // Remove from UI
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.removeNodeFromParent(selectedNode);

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void collectIdsRecursively(int id, sql db, List<Integer> out) throws SQLException {
        out.add(id);
        Directory[] children = db.get("SELECT * FROM nafath_tc_tree WHERE link = ?", id).as(Directory[].class);
        for (Directory child : children) {
            collectIdsRecursively(child.getId(), db, out);
        }
    }
}
