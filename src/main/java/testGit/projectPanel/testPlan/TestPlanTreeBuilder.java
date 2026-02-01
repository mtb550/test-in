package testGit.projectPanel.testPlan;

import com.intellij.ui.CheckedTreeNode;
import lombok.Getter;
import testGit.pojo.Directory;
import testGit.pojo.TestCase;
import testGit.pojo.TestPlan;
import testGit.util.NodeType;
import testGit.util.sql;

import java.util.ArrayList;
import java.util.List;

public class TestPlanTreeBuilder {
    private final TestPlan plan;
    @Getter
    private final List<CheckedTreeNode> allTestCaseNodes = new ArrayList<>();

    public TestPlanTreeBuilder(TestPlan plan) {
        this.plan = plan;
    }

    public CheckedTreeNode buildTree() {
        CheckedTreeNode rootNode = new CheckedTreeNode("Test Cases");
        Directory root = new sql().get("SELECT * FROM nafath_tc_tree WHERE id = ?", plan.getProject_id()).as(Directory.class);
        if (root != null) {
            buildTreeRecursive(root, rootNode);
        }
        return rootNode;
    }

    private void buildTreeRecursive(Directory treeItem, CheckedTreeNode parentNode) {
        CheckedTreeNode currentNode = new CheckedTreeNode(treeItem);
        currentNode.setChecked(false);
        parentNode.add(currentNode);

        if (treeItem.getType() == NodeType.FEATURE.getCode()) {
            TestCase[] testCases = new sql().get("SELECT * FROM nafath_tc WHERE module = ? ORDER BY sort", treeItem.getId()).as(TestCase[].class);
            for (TestCase tc : testCases) {
                CheckedTreeNode testCaseNode = new CheckedTreeNode(tc);
                testCaseNode.setChecked(false);
                allTestCaseNodes.add(testCaseNode);
                currentNode.add(testCaseNode);
            }
            return;
        }

        Directory[] children = new sql().get("SELECT * FROM nafath_tc_tree WHERE link = ?", treeItem.getId()).as(Directory[].class);
        for (Directory child : children) {
            buildTreeRecursive(child, currentNode);
        }
    }

}