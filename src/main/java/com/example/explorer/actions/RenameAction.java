package com.example.explorer.actions;

import com.example.explorer.ExplorerPanel;
import com.example.pojo.Directory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.File;
import java.nio.file.Path;

import static com.example.util.Tools.refreshPath;
import static com.intellij.openapi.actionSystem.PlatformCoreDataKeys.CONTEXT_COMPONENT;

public class RenameAction extends AnAction {
    private final ExplorerPanel panel;

    // استقبال الـ panel عبر الـ Constructor كما فعلنا في DeleteAction
    public RenameAction(final ExplorerPanel panel) {
        super("✏️ Rename");
        this.panel = panel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        JTree tree = e.getData(CONTEXT_COMPONENT) instanceof JTree jTree ? jTree : null;
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (!(node.getUserObject() instanceof Directory treeItem)) return;

        // 1. طلب الاسم الجديد من المستخدم
        String newName = Messages.showInputDialog("Enter new name:", "Rename", null, treeItem.getName(), null);
        if (newName == null || newName.isBlank() || newName.equals(treeItem.getName())) return;

        try {
            File oldFile = treeItem.getFilePath().toFile();
            String newFileName = treeItem.getType() + "_" + treeItem.getId() + "_" + newName;
            Path newFilePath = treeItem.getFilePath().getParent().resolve(newFileName);
            File newFile = newFilePath.toFile();

            if (oldFile.renameTo(newFile)) {
                // 3. تحديث بيانات الكائن (Object)
                treeItem.setName(newName);
                treeItem.setFileName(newFileName);
                treeItem.setFilePath(newFilePath);

                // 4. تحديث الواجهة (Tree & VFS)
                ((DefaultTreeModel) tree.getModel()).nodeChanged(node);
                refreshPath(newFilePath);
                refreshPath(newFilePath.getParent());

                // 5. تحديث الـ ComboBox إذا كان العنصر "مشروع" (Type 0)
                if (treeItem.getType() == 0 && panel.getProjectSelector() != null) {
                    panel.getProjectSelector().reloadProjects();
                }

                System.out.println("Success! Renamed to: " + newFileName);
            } else {
                Messages.showErrorDialog("Could not rename folder. Make sure it's not open in another program.", "Rename Failed");
            }

        } catch (Exception ex) {
            System.err.println("Error during rename: " + ex.getMessage());
        }
    }
}