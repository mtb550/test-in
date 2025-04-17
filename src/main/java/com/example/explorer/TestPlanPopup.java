package com.example.explorer;

import com.example.pojo.TestPlan;
import com.example.pojo.Tree;
import com.example.util.NodeType;
import com.example.util.sql;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class TestPlanPopup {

    public static void showFolderInfo(TestPlan plan, JComponent parent) {
        DialogWrapper dialog = new DialogWrapper(true) {
            private CheckboxTree checkboxTree;
            private CheckedTreeNode rootNode;
            private JTextField buildField;

            {
                init();
                setTitle("Add Test Cases to Plan");
            }

            @Override
            protected JComponent createCenterPanel() {
                JBPanel<?> panel = new JBPanel<>(new BorderLayout(10, 10));
                panel.setPreferredSize(new Dimension(500, 550));

                // === Build Number
                JBPanel<?> buildPanel = new JBPanel<>(new BorderLayout(5, 5));
                buildPanel.add(new JBLabel("🔢 Build Number:"), BorderLayout.NORTH);
                buildField = new JTextField();
                buildPanel.add(buildField, BorderLayout.CENTER);
                panel.add(buildPanel, BorderLayout.NORTH);

                // === Load Test Case Tree
                rootNode = new CheckedTreeNode("Test Cases");
                Tree root = new sql().get("SELECT * FROM tree WHERE id = ?", plan.getProject_id()).as(Tree.class);
                if (root != null) {
                    buildTreeRecursive(root, rootNode);
                }

                checkboxTree = new CheckboxTree(new CheckboxTree.CheckboxTreeCellRenderer() {
                    @Override
                    public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
                        if (value instanceof CheckedTreeNode ctNode) {
                            Object userObject = ctNode.getUserObject();
                            if (userObject instanceof Tree treeNode) {
                                getTextRenderer().append(treeNode.getName());
                            } else {
                                getTextRenderer().append(value.toString());
                            }
                        }
                    }
                }, rootNode);

                JBScrollPane scrollPane = new JBScrollPane(checkboxTree);
                scrollPane.setPreferredSize(new Dimension(480, 380));
                panel.add(scrollPane, BorderLayout.CENTER);

                // === Add Button
                JButton addButton = new JButton("➕ Add");
                addButton.addActionListener(e -> onAdd(plan));
                JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                bottomPanel.add(addButton);
                panel.add(bottomPanel, BorderLayout.SOUTH);

                return panel;
            }

            private void onAdd(TestPlan plan) {
                String buildNumber = buildField.getText().trim();
                if (buildNumber.isBlank()) {
                    Messages.showWarningDialog("Please enter a build number.", "Validation");
                    return;
                }

                List<Integer> selectedCaseIds = new ArrayList<>();
                collectSelectedFeatures(rootNode, selectedCaseIds);

                if (selectedCaseIds.isEmpty()) {
                    Messages.showWarningDialog("Please select at least one test case.", "Validation");
                    return;
                }

                sql db = new sql();
                db.execute("UPDATE nafath_tp_tree SET build_number = ? WHERE id = ?", buildNumber, plan.getId());

                for (int i = 0; i < selectedCaseIds.size(); i++) {
                    db.execute("INSERT INTO nafath_tp_testcases (plan_id, test_case_id, run_order) VALUES (?, ?, ?)",
                            plan.getId(), selectedCaseIds.get(i), i + 1);
                }

                Messages.showInfoMessage("Test cases added to plan successfully.", "Success");
                close(OK_EXIT_CODE);
            }

            private void buildTreeRecursive(Tree treeItem, CheckedTreeNode parentNode) {
                CheckedTreeNode currentNode = new CheckedTreeNode(treeItem);
                parentNode.add(currentNode);

                Tree[] children = new sql().get("SELECT * FROM tree WHERE link = ?", treeItem.getId()).as(Tree[].class);
                for (Tree child : children) {
                    buildTreeRecursive(child, currentNode);
                }
            }

            private void collectSelectedFeatures(CheckedTreeNode node, List<Integer> output) {
                Enumeration<?> enumeration = node.children();
                while (enumeration.hasMoreElements()) {
                    Object child = enumeration.nextElement();
                    if (child instanceof CheckedTreeNode ctNode) {
                        Object userObject = ctNode.getUserObject();
                        if (ctNode.isChecked() && userObject instanceof Tree treeNode && treeNode.getType() == NodeType.FEATURE.getCode()) {
                            output.add(treeNode.getId());
                        }
                        collectSelectedFeatures(ctNode, output);
                    }
                }
            }

            private void setChildrenChecked(CheckedTreeNode parent, boolean checked) {
                Enumeration<?> enumeration = parent.children();
                while (enumeration.hasMoreElements()) {
                    Object child = enumeration.nextElement();
                    if (child instanceof CheckedTreeNode ctNode) {
                        ctNode.setChecked(checked);
                        setChildrenChecked(ctNode, checked);
                    }
                }
                checkboxTree.repaint();
            }
        };

        dialog.show();
    }
}