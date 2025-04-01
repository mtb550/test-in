package com.example.explorer;

import com.example.editor.TestCaseEditor;
import com.example.pojo.DB;
import com.example.pojo.Feature;
import com.example.pojo.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.List;

public class TestTreePanel {
    private JBPanel mainPanel;
    private Tree tree;

    public TestTreePanel() {
        mainPanel = new JBPanel(new BorderLayout());

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Test Projects");
        List<Project> projects = DB.loadProjects();
        for (Project project : projects) {
            DefaultMutableTreeNode projectNode = new DefaultMutableTreeNode(project.getName());
            for (Feature feature : project.getFeatures()) {
                projectNode.add(new DefaultMutableTreeNode(new FeatureNodeData(project.getName(), feature)));
            }
            root.add(projectNode);
        }

        tree = new Tree(root);
        JBScrollPane scrollPane = new JBScrollPane(tree);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selected = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (selected == null || !(selected.getUserObject() instanceof FeatureNodeData)) return;

            FeatureNodeData data = (FeatureNodeData) selected.getUserObject();
            SwingUtilities.invokeLater(() -> TestCaseEditor.open(data.projectName, data.feature));
        });
    }

    public JBPanel getMainPanel() {
        return mainPanel;
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
