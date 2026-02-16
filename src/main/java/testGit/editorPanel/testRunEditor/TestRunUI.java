package testGit.editorPanel.testRunEditor;

import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBScrollPane;
import lombok.Getter;
import testGit.pojo.Directory;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class TestRunUI {
    private CheckboxTree checklistTree;

    public JComponent createEditorPanel(DefaultTreeModel testCaseModel, String savePathString) {
        CheckedTreeNode root = convertToCheckedNodes((DefaultMutableTreeNode) testCaseModel.getRoot());

        checklistTree = new CheckboxTree(new CheckboxTree.CheckboxTreeCellRenderer() {
            @Override
            public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                if (value instanceof CheckedTreeNode node && node.getUserObject() instanceof Directory dir) {
                    getTextRenderer().append(dir.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                }
            }
        }, root);

        com.intellij.util.ui.tree.TreeUtil.expandAll(checklistTree);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JBScrollPane(checklistTree), BorderLayout.CENTER);

        JButton saveButton = new JButton("Save Test Run");
        saveButton.addActionListener(e -> saveSelectedToJSON(root, savePathString));
        panel.add(saveButton, BorderLayout.SOUTH);

        return panel;
    }

    private void saveSelectedToJSON(CheckedTreeNode root, String savePath) {
        List<String> selectedFiles = new ArrayList<>();
        collectCheckedItems(root, selectedFiles);
        System.out.println("Saving to: " + savePath);
        selectedFiles.forEach(System.out::println);
    }

    private void collectCheckedItems(CheckedTreeNode node, List<String> paths) {
        if (node.isChecked() && node.isLeaf() && node.getUserObject() instanceof Directory dir) {
            paths.add(dir.getFilePath().toString());
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectCheckedItems((CheckedTreeNode) node.getChildAt(i), paths);
        }
    }

    private CheckedTreeNode convertToCheckedNodes(DefaultMutableTreeNode node) {
        CheckedTreeNode newNode = new CheckedTreeNode(node.getUserObject());
        for (int i = 0; i < node.getChildCount(); i++) {
            newNode.add(convertToCheckedNodes((DefaultMutableTreeNode) node.getChildAt(i)));
        }
        return newNode;
    }
}