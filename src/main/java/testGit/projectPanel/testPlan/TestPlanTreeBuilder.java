package testGit.projectPanel.testPlan;

import com.intellij.ui.CheckedTreeNode;
import lombok.Getter;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;
import testGit.pojo.TestCase;
import testGit.pojo.TestPlan;

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
        Directory root = null;
        if (root != null) {
            buildTreeRecursive(root, rootNode);
        }
        return rootNode;
    }

    private void buildTreeRecursive(Directory treeItem, CheckedTreeNode parentNode) {
        CheckedTreeNode currentNode = new CheckedTreeNode(treeItem);
        currentNode.setChecked(false);
        parentNode.add(currentNode);

        if (treeItem.getType() == DirectoryType.F) {
            TestCase[] testCases = null;
            for (TestCase tc : testCases) {
                CheckedTreeNode testCaseNode = new CheckedTreeNode(tc);
                testCaseNode.setChecked(false);
                allTestCaseNodes.add(testCaseNode);
                currentNode.add(testCaseNode);
            }
            return;
        }

        Directory[] children = null;
        for (Directory child : children) {
            buildTreeRecursive(child, currentNode);
        }
    }

}