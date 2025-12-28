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
                rootNode.add(buildSubTree2(child));
            }
        }

        treeModel = new DefaultTreeModel(rootNode);
        treeModel.addTreeModelListener(new ReloadAllOnInsertListener());
    }

    static DefaultMutableTreeNode buildSubTree2(File folder) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(folder.getName());

        System.out.println("#122 " + folder.getName());

        File[] children = folder.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    node.add(buildSubTree2(child));
                }
            }
        }

        return node;
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