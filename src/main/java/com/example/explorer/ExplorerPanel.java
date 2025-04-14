package com.example.explorer;

import com.example.pojo.Tree;
import com.example.util.ShortcutRegistry;
import com.example.util.sql;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

@Getter
public class ExplorerPanel {
    private final JPanel panel;
    private final SimpleTree tree;

    public ExplorerPanel() {
        panel = new JPanel(new BorderLayout());
        tree = new SimpleTree();
        ExplorerTree.build();

        tree.setModel(ExplorerTree.getTreeModel());

        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new IntelliJRenderer());
        tree.addMouseListener(new TestCaseTreeMouseAdapter(tree));
        TestCaseTreeKeyAdapter.register(tree, ProjectManager.getInstance().getOpenProjects()[0]);

        // enable drag and drop
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setTransferHandler(new TreeTransferHandler(tree));

        // register all keyboard shortcuts
        ShortcutRegistry.Explorer(tree);

        // tree scroll view
        JBScrollPane scrollPane = new JBScrollPane(tree);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // add a dropdown selector (below toolbar)
        //JPanel topBar = new JPanel(new BorderLayout());
        //topBar.add(new ComboBoxProjectSelector(this).getComponent(), BorderLayout.WEST);

        //panel.add(topBar, BorderLayout.NORTH);
        panel.add(new ComboBoxProjectSelector(this).getComponent(), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

    }

    public void loadAllProjects() {
        ExplorerTree.build(); // Rebuild the whole tree
        tree.setModel(ExplorerTree.getTreeModel());
        tree.setRootVisible(true);
        //tree.expandRow(0);
    }

    public void filterByProject(int projectId) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Test Cases");
        Tree selectedProject = new sql().get("SELECT * FROM tree WHERE id = ?", projectId).as(Tree.class);
        DefaultMutableTreeNode node = ExplorerTree.buildSubTree(selectedProject);
        root.add(node);

        ExplorerTree.treeModel = new DefaultTreeModel(root);
        tree.setModel(ExplorerTree.treeModel);
        tree.setRootVisible(true);
        //tree.expandRow(0);
    }

    static class IntelliJRenderer implements TreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            SimpleColoredComponent comp = new SimpleColoredComponent();
            comp.setOpaque(false);
            comp.setBorder(null);

            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            String text = value.toString();

            if (userObject instanceof Tree treeItem) {
                text = treeItem.getName();
                switch (treeItem.getType()) {
                    case 0 -> comp.setIcon(AllIcons.Nodes.Project);
                    case 1 -> comp.setIcon(AllIcons.Nodes.Folder);
                    case 2 -> comp.setIcon(AllIcons.Nodes.Class);
                }

                // dim color for cut nodes
                if (TestCaseTreeKeyAdapter.isCutNode(treeItem.getId())) {
                    comp.append(text, SimpleTextAttributes.GRAYED_ATTRIBUTES);
                } else {
                    comp.append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
                }
            } else {
                comp.append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }

            return comp;
        }
    }


}
