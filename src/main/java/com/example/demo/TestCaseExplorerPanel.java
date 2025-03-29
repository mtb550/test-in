package com.example.demo;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class TestCaseExplorerPanel {
    private final JPanel panel;
    private final JTree tree;

    public TestCaseExplorerPanel() {
        panel = new JPanel(new BorderLayout());

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Test Cases");

        List<Project> projects = DB.loadProjects();
        for (Project project : projects) {
            DefaultMutableTreeNode projectNode = new DefaultMutableTreeNode(project.getName());
            for (Feature feature : project.getFeatures()) {
                DefaultMutableTreeNode featureNode = new DefaultMutableTreeNode(new FeatureNodeData(project.getName(), feature));
                projectNode.add(featureNode);
            }
            root.add(projectNode);
        }

        tree = new JTree(root);
        JScrollPane scrollPane = new JScrollPane(tree);
        panel.add(scrollPane, BorderLayout.CENTER);

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                        Object userObject = selectedNode.getUserObject();
                        if (userObject instanceof FeatureNodeData data) {
                            TestCaseEditor.open(data.projectName, data.feature);
                        }
                    }
                }
            }
        });
    }

    public JPanel getPanel() {
        return panel;
    }

    static class FeatureNodeData {
        String projectName;
        Feature feature;

        FeatureNodeData(String projectName, Feature feature) {
            this.projectName = projectName;
            this.feature = feature;
        }

        public String toString() {
            return feature.getName();
        }
    }
}
