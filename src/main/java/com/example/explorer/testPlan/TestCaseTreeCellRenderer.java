package com.example.explorer.testPlan;

import com.example.pojo.Directory;
import com.example.pojo.GroupType;
import com.example.pojo.TestCase;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.SimpleTextAttributes;

import javax.swing.*;
import java.util.stream.Collectors;

public class TestCaseTreeCellRenderer extends CheckboxTree.CheckboxTreeCellRenderer {
    @Override
    public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded,
                                  boolean leaf, int row, boolean hasFocus) {
        if (value instanceof CheckedTreeNode ctNode) {
            Object userObject = ctNode.getUserObject();
            if (userObject instanceof Directory treeNode) {
                getTextRenderer().append(treeNode.getName());
            } else if (userObject instanceof TestCase testCase) {
                renderTestCase(testCase);
            } else {
                getTextRenderer().append(value.toString());
            }
        }
    }

    private void renderTestCase(TestCase testCase) {
        getTextRenderer().append("🧪 " + testCase.getTitle());
        if (testCase.getGroups() != null && !testCase.getGroups().isEmpty()) {
            getTextRenderer().append("  [");
            // Convert List<GroupType> to comma-separated string of names
            String groupNames = testCase.getGroups().stream()
                    .map(GroupType::name)
                    .collect(Collectors.joining(", "));
            getTextRenderer().append(groupNames);
            getTextRenderer().append("]", SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
    }
}