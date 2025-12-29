package com.example.explorer;

import com.example.pojo.Directory;
import com.example.pojo.Tree;
import lombok.Getter;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;

public class ExplorerTree {

    @Getter
    public static DefaultTreeModel treeModel;

    public static void buildTree() {
        // Absolute path from project root.
        /// to be updated to get file path from config file or dynamically.
        File rootFolder = new File("/home/mtb/IdeaProjects/untitled/TestGit");

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("TEST CASES A");

        for (File child : rootFolder.listFiles()) {
            if (child.isDirectory()) {
                Tree tree = mapFolderToTree(child);
                rootNode.add(buildSubTree(tree));
            }
        }

        treeModel = new DefaultTreeModel(rootNode);
        treeModel.addTreeModelListener(new ReloadAllOnInsertListener());
    }

    static DefaultMutableTreeNode buildSubTree(Directory folder) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(folder.getName());

        File[] children = folder.getFile().listFiles();

        if (children != null) {
            for (File childFile : children) {
                if (childFile.isDirectory()) {
                    Tree childTree = mapFolderToTree(childFile);
                    node.add(buildSubTree(childTree));
                }
            }
        }

        return node;
    }

    // دالة مساعدة لتحويل File إلى Tree بناءً على الاسم (ID_Name_Type)
    private static Tree mapFolderToTree(File file) {
        String fullName = file.getName();
        String[] parts = fullName.split("_", 3);

        Tree t = new Tree();
        t.setFile(file); // تخزين مرجع الملف الفعلي
        t.setName(fullName); // اسم افتراضي في حال فشل التقسيم

        try {
            if (parts.length >= 2) {
                t.setId(Integer.parseInt(parts[0]));
                t.setName(parts[1]); // الاسم النظيف بدون أرقام
                if (parts.length > 2) {
                    t.setType(Integer.parseInt(parts[2]));
                }
            }
        } catch (NumberFormatException e) {
            // في حال فشل تحويل الرقم، يظل الاسم هو fullName
        }

        return t;
    }

    private static class ReloadAllOnInsertListener implements TreeModelListener {
        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            treeModel.reload();
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            treeModel.reload();

        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            treeModel.reload();

        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            treeModel.reload();
        }

    }
}