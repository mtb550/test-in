package com.example.explorer;

import com.example.editor.TestCaseEditor;
import com.example.pojo.DB;
import com.example.pojo.Feature;
import com.example.pojo.Project;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class TestCaseExplorerPanel {
    private final NonOpaquePanel toolWindowPanel;
    private final SimpleTree tree;

    public TestCaseExplorerPanel() {
        toolWindowPanel = new NonOpaquePanel(new BorderLayout());
        tree = new SimpleTree();


        tree.setModel(buildTreeModel());
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new IntelliJRenderer());


        //new TreeSpeedSearch(tree);

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        Object userObject = node.getUserObject();
                        if (userObject instanceof NodeInfo info && info.type == NodeType.FEATURE) {
                            Feature feature = DB.getFeature(info.projectName, info.name);
                            if (feature != null) {
                                TestCaseEditor.open(info.projectName, feature);
                            }
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        tree.setSelectionPath(path);
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        showContextMenu(e, node);
                    }
                }
            }
        });

        JBScrollPane scrollPane = new JBScrollPane(tree);
        //toolWindowPanel.setContent(scrollPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());


        // Add IntelliJ-style toolbar
        toolWindowPanel.add(buildToolbar(), BorderLayout.NORTH);
        toolWindowPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private DefaultTreeModel buildTreeModel() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Test Cases");
        List<Project> projects = DB.loadProjects();
        for (Project project : projects) {
            DefaultMutableTreeNode projectNode = new DefaultMutableTreeNode(new NodeInfo(project.getName(), NodeType.PROJECT));
            for (Feature feature : project.getFeatures()) {
                DefaultMutableTreeNode featureNode = new DefaultMutableTreeNode(new NodeInfo(feature.getName(), NodeType.FEATURE, project.getName()));
                projectNode.add(featureNode);
            }
            root.add(projectNode);
        }
        return new DefaultTreeModel(root);
    }

    private JComponent buildToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(new AnAction("Expand All", "Expand all nodes", AllIcons.Actions.Expandall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
            }
        });

        group.add(new AnAction("Collapse All", "Collapse all nodes", AllIcons.Actions.Collapseall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                for (int i = tree.getRowCount() - 1; i >= 0; i--) tree.collapseRow(i);
            }
        });

        group.addSeparator();

        group.add(new AnAction("Refresh", "Reload", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                tree.setModel(buildTreeModel());
            }
        });

        group.add(new AnAction("Settings", "Configure Tree", AllIcons.General.Settings) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                JOptionPane.showMessageDialog(tree, "Settings coming soon!");
            }
        });

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("TestExplorerToolbar", group, true);
        toolbar.setTargetComponent(tree);

        NonOpaquePanel panel = new NonOpaquePanel(new BorderLayout());
        panel.add(toolbar.getComponent(), BorderLayout.WEST);
        panel.setBorder(null);

        return panel;
    }

    private void showContextMenu(MouseEvent e, DefaultMutableTreeNode node) {
        ActionGroup actionGroup = (ActionGroup) ActionManager.getInstance().getAction("TestTreeContextMenuGroup");
        ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.PROJECT_VIEW_POPUP, actionGroup);
        popupMenu.getComponent().show(tree, e.getX(), e.getY());
    }

    public JPanel getPanel() {
        return toolWindowPanel;
    }

    enum NodeType {PROJECT, FOLDER, FEATURE}

    static class NodeInfo {
        String name;
        String projectName;
        NodeType type;

        NodeInfo(String name, NodeType type) {
            this(name, type, null);
        }

        NodeInfo(String name, NodeType type, String projectName) {
            this.name = name;
            this.type = type;
            this.projectName = projectName;
        }

        public String toString() {
            return name;
        }
    }

    static class IntelliJRenderer implements TreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            SimpleColoredComponent comp = new SimpleColoredComponent();
            comp.setOpaque(false);
            comp.setBorder(null);

            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            String text = value.toString();

            if (userObject instanceof NodeInfo info) {
                text = info.name;
                switch (info.type) {
                    case PROJECT -> comp.setIcon(AllIcons.Nodes.Module);
                    case FOLDER -> comp.setIcon(AllIcons.Nodes.Folder);
                    case FEATURE -> comp.setIcon(AllIcons.Nodes.Test);
                }
            }

            comp.append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);

            return comp;
        }
    }
}
