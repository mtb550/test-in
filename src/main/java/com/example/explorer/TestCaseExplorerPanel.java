package com.example.explorer;

import com.example.pojo.Tree;
import com.example.util.ShortcutRegistry;
import com.example.util.sql;
import com.intellij.icons.AllIcons;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.treeStructure.SimpleTree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class TestCaseExplorerPanel {
    private final NonOpaquePanel toolWindowPanel;
    private final SimpleTree tree;
    private sql db = new sql();

    public TestCaseExplorerPanel() {
        toolWindowPanel = new NonOpaquePanel(new BorderLayout());
        tree = new SimpleTree();

        tree.setModel(buildTreeModel());
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new IntelliJRenderer());
        tree.addMouseListener(new TestCaseTreeMouseAdapter(tree));

        // Enable drag and drop
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setTransferHandler(new TreeTransferHandler(tree));

        // register all keyboard shortcuts
        ShortcutRegistry.Explorer(tree);

        JBScrollPane scrollPane = new JBScrollPane(tree);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        toolWindowPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private DefaultTreeModel buildTreeModel() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Test Cases");

        Tree[] rootNodes = db.get("SELECT * FROM tree WHERE type = 0").as(Tree[].class);

        for (Tree treeItem : rootNodes) {
            DefaultMutableTreeNode node = buildSubTree(treeItem);
            root.add(node);
        }

        return new DefaultTreeModel(root);
    }

    private DefaultMutableTreeNode buildSubTree(Tree treeItem) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(
                new NodeInfo(treeItem.getName(), treeItem.getType(), treeItem.getId())
        );

        Tree[] children = db.get("SELECT * FROM tree WHERE link = ?", treeItem.getId()).as(Tree[].class);

        for (Tree child : children) {
            node.add(buildSubTree(child));
        }

        return node;
    }


    public JPanel getPanel() {
        return toolWindowPanel;
    }

    public void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
    }

    public void collapseAll() {
        for (int i = tree.getRowCount() - 1; i >= 0; i--) tree.collapseRow(i);
    }

    public void refresh() {
        tree.setModel(buildTreeModel());
    }

    static class NodeInfo {
        String name;
        Integer type;
        Integer id;
        Integer link;

        NodeInfo(String name, int type, int id, int link) {
            this.name = name;
            this.type = type;
            this.id = id;
            this.link = link;
        }

        NodeInfo(String name, int type, int id) {
            this.name = name;
            this.type = type;
            this.id = id;
        }

        public String toString() {
            return name;
        }
    }

    static class IntelliJRenderer implements TreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            SimpleColoredComponent comp = new SimpleColoredComponent();
            comp.setOpaque(false);
            comp.setBorder(null);

            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            String text = value.toString();

            if (userObject instanceof NodeInfo info) {
                text = info.name;
                switch (info.type) {
                    case 0 -> comp.setIcon(AllIcons.Nodes.Project);
                    case 1 -> comp.setIcon(AllIcons.Nodes.Folder);
                    case 2 -> comp.setIcon(AllIcons.Nodes.Class);
                }
            }

            comp.append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            return comp;
        }
    }
}
