package testGit.editorPanel.testRunEditor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellij.openapi.Disposable;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.Getter;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;
import testGit.pojo.TestCase;
import testGit.util.TestCaseSorter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
public class TestRunUI implements Disposable {
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final List<TestCase> initialTestCases;
    private CheckboxTree checklistTree;

    public TestRunUI(List<TestCase> initialTestCases) {
        this.initialTestCases = TestCaseSorter.sortTestCases(initialTestCases);
    }

    /**
     * Creates the editor panel containing the checklist of test cases.
     *
     * @param testCaseModel  The ready-made hierarchy model from ProjectPanel.
     * @param savePathString The path where the test run will be saved.
     */
    public JComponent createEditorPanel(DefaultTreeModel testCaseModel, String savePathString) {
        JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());

        // 1. Convert the existing hierarchy model to a Checked Tree, injecting JSON files
        CheckedTreeNode root = convertToCheckedNodes((DefaultMutableTreeNode) testCaseModel.getRoot());

        // 2. Setup CheckboxTree with a renderer for both Directories and TestCases
        checklistTree = new CheckboxTree(new CheckboxTree.CheckboxTreeCellRenderer() {
            @Override
            public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                if (value instanceof CheckedTreeNode node) {
                    Object userObj = node.getUserObject();
                    if (userObj instanceof Directory dir) {
                        getTextRenderer().append(dir.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    } else if (userObj instanceof TestCase tc) {
                        // Display the Test Case title instead of the file path
                        String title = tc.getTitle() != null ? tc.getTitle() : "Unnamed Test";
                        getTextRenderer().append(title, SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    }
                }
            }
        }, root);

        TreeUtil.expandAll(checklistTree);

        mainPanel.add(new JBScrollPane(checklistTree), BorderLayout.CENTER);

        JButton saveButton = new JButton("Save Test Run");
        saveButton.addActionListener(e -> saveSelectedToJSON(root, savePathString));
        mainPanel.add(saveButton, BorderLayout.SOUTH);

        return mainPanel;
    }

    /**
     * Recursively clones the tree structure and injects Test Cases under TS folders.
     */
    private CheckedTreeNode convertToCheckedNodes(DefaultMutableTreeNode node) {
        Object userObj = node.getUserObject();
        CheckedTreeNode newNode = new CheckedTreeNode(userObj);

        // 1. Copy sub-directories from the original model
        for (int i = 0; i < node.getChildCount(); i++) {
            newNode.add(convertToCheckedNodes((DefaultMutableTreeNode) node.getChildAt(i)));
        }

        // 2. Inject Test Cases (JSON files) if this node is a Test Set (TS)
        if (userObj instanceof Directory dir && dir.getType() == DirectoryType.TS) {
            List<TestCase> cases = loadTestCasesFromDir(dir);
            for (TestCase tc : cases) {
                CheckedTreeNode tcNode = new CheckedTreeNode(tc);

                // Pre-check the node if it's part of the existing run
                if (initialTestCases != null && isAlreadyInRun(tc)) {
                    tcNode.setChecked(true);
                }

                newNode.add(tcNode);
            }
        }

        return newNode;
    }

    private boolean isAlreadyInRun(TestCase tc) {
        return initialTestCases.stream()
                .anyMatch(existing -> existing.getUid() == tc.getUid());
    }

    private List<TestCase> loadTestCasesFromDir(Directory dir) {
        List<TestCase> testCases = new ArrayList<>();
        File folder = dir.getFile();
        if (folder != null && folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((d, name) -> name.toLowerCase().endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try {
                        testCases.add(mapper.readValue(file, TestCase.class));
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        // FIX: Apply the sorter before returning the list to the tree builder
        return TestCaseSorter.sortTestCases(testCases);
    }

    private void saveSelectedToJSON(CheckedTreeNode root, String savePath) {
        // Logic to collect all checked TestCase nodes and save them to the run file
    }

    @Override
    public void dispose() {
    }
}