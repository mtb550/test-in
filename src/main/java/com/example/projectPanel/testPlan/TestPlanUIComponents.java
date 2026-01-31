package com.example.projectPanel.testPlan;

import com.example.pojo.TestPlan;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.treeStructure.SimpleTree;

import javax.swing.*;

public class TestPlanUIComponents {
    private final TestPlan plan;
    private final SimpleTree parent;
    private final TestPlanTreeBuilder treeBuilder;
    private final GroupFilterHandler groupFilterHandler;
    private final ConfigPanel configPanel;
    private final TestPlanActionHandler actionHandler;

    private CheckboxTree checkboxTree;
    private CheckedTreeNode rootNode;
    private JBTextField buildField;

    public TestPlanUIComponents(TestPlan plan, SimpleTree parent) {
        this.plan = plan;
        this.parent = parent;
        this.treeBuilder = new TestPlanTreeBuilder(plan);
        this.groupFilterHandler = new GroupFilterHandler();
        this.configPanel = new ConfigPanel();
        this.actionHandler = new TestPlanActionHandler(plan, configPanel);
    }

    public JComponent createTopPanel() {
        JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(actionHandler.createBuildNumberPanel());
        panel.add(groupFilterHandler.createToolbar());
        return panel;
    }

    public JComponent createTreePanel() {
        rootNode = treeBuilder.buildTree();
        checkboxTree = new CheckboxTree(new TestCaseTreeCellRenderer(), rootNode);

        for (int i = 0; i < checkboxTree.getRowCount(); i++) {
            checkboxTree.expandRow(i);
        }

        groupFilterHandler.setCheckboxTree(checkboxTree);
        groupFilterHandler.setAllTestCaseNodes(treeBuilder.getAllTestCaseNodes()); // Add this line
        actionHandler.setCheckboxTree(checkboxTree);
        actionHandler.setRootNode(rootNode);

        return new JBScrollPane(checkboxTree);
    }

    public JComponent createConfigPanel() {
        return configPanel.createPanel();
    }

    public void handleOkAction() {
        actionHandler.handleOkAction(parent);
    }
}