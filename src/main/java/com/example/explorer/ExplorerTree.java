package com.example.explorer;

import com.example.pojo.Directory;
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
                Directory tree = mapFolderToDirectory(child);
                rootNode.add(buildSubTree(tree));
            }
        }

        treeModel = new DefaultTreeModel(rootNode);
        treeModel.addTreeModelListener(new ReloadAllOnInsertListener());
    }

    static DefaultMutableTreeNode buildSubTree(Directory folder) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(folder);

        File[] children = folder.getFile().listFiles();

        if (children != null) {
            for (File childFile : children) {
                if (childFile.isDirectory()) {
                    Directory childTree = mapFolderToDirectory(childFile);
                    node.add(buildSubTree(childTree));
                }
            }
        }

        return node;
    }

    // دالة مساعدة لتحويل File إلى Tree بناءً على الاسم (ID_Name_Type)
    private static Directory mapFolderToDirectory(File file) {
        String fullName = file.getName();
        String[] parts = fullName.split("_", 3);

        Directory t = new Directory();
        t.setFile(file); // تخزين مرجع الملف الفعلي
        //t.setName(fullName); // اسم افتراضي في حال فشل التقسيم
        t.setId(Integer.parseInt(parts[0]));
        t.setName(parts[1]); // الاسم النظيف بدون أرقام
        t.setType(Integer.parseInt(parts[2]));

        return t;
    }

    private static class ReloadAllOnInsertListener implements TreeModelListener {
        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            //treeModel.reload();
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            // treeModel.reload();

        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            //  treeModel.reload();

        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            //   treeModel.reload();
        }

    }
}