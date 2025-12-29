package com.example.explorer;

import com.example.pojo.Tree;
import com.example.util.sql;
import lombok.Getter;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;

public class ExplorerTree {

    @Getter
    public static DefaultTreeModel treeModel;

    public static void build() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Test Cases");

        Tree[] rootNodes = new sql().get("SELECT * FROM nafath_tc_tree WHERE link = 0").as(Tree[].class);

        for (Tree treeItem : rootNodes) {
            DefaultMutableTreeNode node = buildSubTree(treeItem);
            root.add(node);
        }

        treeModel = new DefaultTreeModel(root);
        treeModel.addTreeModelListener(new ReloadAllOnInsertListener());
    }

    static DefaultMutableTreeNode buildSubTree(Tree treeItem) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(treeItem);

        Tree[] children = new sql().get("SELECT * FROM nafath_tc_tree WHERE link = ?", treeItem.getId()).as(Tree[].class);

        for (Tree childItem : children) {
            node.add(buildSubTree(childItem));
        }

        return node;
    }

    public static void build_NEW() {
        // Absolute path from project root
        String projectRoot = System.getProperty("user.dir");
        File rootFolder = new File("/home/mtb/IdeaProjects/untitled/TestGit");

        if (!rootFolder.exists()) {
            System.out.println("❌ Folder not found!");
            System.out.println(projectRoot);
            System.out.println("Looking at: " + rootFolder.getAbsolutePath());
            System.out.println("❌ Folder not found!");
            return;
        }

        System.out.println("✓ Found Test Cases folder");
        System.out.println("Path: " + rootFolder.getAbsolutePath());
        System.out.println("rootFolder.getName(): "+rootFolder.getName());

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("TEST CASES A");

        for (File child : rootFolder.listFiles()) {
            if (child.isDirectory()) {
                String[] name = child.getName().split("_");
                Tree tree = new Tree()
                        .setFile(child)
                        .setId(Integer.parseInt(name[0]))
                        .setType(Integer.parseInt(name[2]))
                        .setName(name[1]);

                rootNode.add(buildSubTree2(tree));
            }
        }

        treeModel = new DefaultTreeModel(rootNode);
        treeModel.addTreeModelListener(new ReloadAllOnInsertListener());
    }

    static DefaultMutableTreeNode buildSubTree2(Tree folder) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(folder);

        System.out.println("#122 " + folder.getName());

        File[] children = folder.getFile().listFiles();
        if (children != null) {
            for (File childFile : children) {
                if (childFile.isDirectory()) {
                    Tree childTree = mapFolderToTree(childFile);
                    node.add(buildSubTree2(childTree));
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
            //treeModel.reload();
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            //treeModel.reload();

        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            //treeModel.reload();

        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            //treeModel.reload();

        }
    }
}