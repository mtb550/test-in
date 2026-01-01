package com.example.explorer.actions;

import com.example.explorer.ComboBoxProjectSelector;
import com.example.explorer.ExplorerPanel;
import com.example.pojo.Directory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.File;

import static com.example.util.Tools.refreshPath;
import static com.intellij.openapi.actionSystem.PlatformCoreDataKeys.CONTEXT_COMPONENT;

public class DeleteAction extends AnAction {
    private final ExplorerPanel panel;

    public DeleteAction(final ExplorerPanel panel) {
        super("❌ Delete");
        this.panel = panel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        JTree tree = e.getData(CONTEXT_COMPONENT) instanceof JTree jTree ? jTree : null;
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (!(selectedNode.getUserObject() instanceof Directory treeItem)) return;

        int confirm = Messages.showYesNoDialog(
                "Are you sure you want to delete '" + treeItem.getName() + "' and all its folders/files on disk?",
                "Confirm Delete",
                Messages.getQuestionIcon()
        );
        if (confirm != Messages.YES) return;

        try {
            File fileOnDisk = treeItem.getFilePath().toFile();
            if (fileOnDisk.exists()) {
                boolean success = FileUtil.delete(fileOnDisk);

                if (success) {
                    System.out.println("Success! Deleted: " + treeItem.getFilePath());
                    refreshPath(treeItem.getFilePath().getParent());

                } else {
                    System.out.println("Could not delete folder. It might be in use. Delete Failed.");
                    return;
                }
            }

            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.removeNodeFromParent(selectedNode);

            // بعد نجاح الحذف، حدث الـ ComboBox بسهولة
            if (panel.getProjectSelector() != null) {
                panel.getProjectSelector().reloadProjects();
                panel.loadAllProjects();
                panel.filterByProject(ComboBoxProjectSelector.comboBox.getItem());
            }

        } catch (Exception ex) {
            //Messages.showErrorDialog("Error during delete: " + ex.getMessage(), "Error");
            System.out.println("Error during delete: " + ex.getMessage());
        }
    }
}