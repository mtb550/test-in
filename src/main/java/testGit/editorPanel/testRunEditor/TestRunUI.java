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
import testGit.pojo.*;
import testGit.util.TestCaseSorter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class TestRunUI implements Disposable {
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final List<TestCase> initialTestCases;
    private CheckboxTree checklistTree;
    private TestRun currentTestRun;

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
                        // Find matching execution result from our TestRun object
                        TestRun.TestRunItems result = findResultFor(tc.getId());

                        SimpleTextAttributes mainStyle = SimpleTextAttributes.REGULAR_ATTRIBUTES;
                        String statusText = " [Pending]";

                        if (result != null) {
                            switch (result.getStatus()) {
                                case "PASSED" -> {
                                    mainStyle = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, new Color(36, 138, 61));
                                    statusText = " [Passed]";
                                }
                                case "FAILED" -> {
                                    mainStyle = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, Color.RED);
                                    statusText = " [Failed]";
                                }
                                case "BLOCKED" -> {
                                    mainStyle = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, Color.ORANGE);
                                    statusText = " [Blocked]";
                                }
                            }
                        }

                        getTextRenderer().append(tc.getTitle(), mainStyle);
                        getTextRenderer().append(statusText, SimpleTextAttributes.GRAYED_ATTRIBUTES);
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

    private void saveSelectedToJSON(CheckedTreeNode root, String baseProjectPath) {
        // 1. Create the 'testRuns' directory path
        File testRunsDir = new File(baseProjectPath, "testRuns");

        // 2. Ensure the directory exists
        if (!testRunsDir.exists()) {
            testRunsDir.mkdirs();
        }

        // 3. Define the actual filename (e.g., using the run name or a timestamp)
        // For now, we'll use a default name, but you should probably let the user name it
        String fileName = "Run_" + System.currentTimeMillis() + ".json";
        File finalOutputFile = new File(testRunsDir, fileName);

        TestRun run = new TestRun();
        run.setRunName(fileName);
        run.setCreatedAt(LocalDateTime.now());
        run.setStatus(TestRunStatus.CREATED);

        List<TestRun.TestRunItems> items = new ArrayList<>();
        collectCheckedItems(root, items);
        run.setResults(items);

        try {
            // FIX: Write to the FILE object, not the DIRECTORY object
            mapper.writerWithDefaultPrettyPrinter().writeValue(finalOutputFile, run);
            System.out.println("Test Run saved successfully to: " + finalOutputFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void collectCheckedItems(CheckedTreeNode node, List<TestRun.TestRunItems> items) {
        if (node.getUserObject() instanceof TestCase tc && node.isChecked()) {
            TestRun.TestRunItems item = new TestRun.TestRunItems();
            // Assuming tc.getId() returns a string that can be converted to UUID
            item.setTestCaseId(UUID.fromString(tc.getId()));
            item.setStatus("PENDING"); // Default status for new run
            items.add(item);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            collectCheckedItems((CheckedTreeNode) node.getChildAt(i), items);
        }
    }

    /**
     * Finds the execution result for a specific test case by its ID.
     */
    private TestRun.TestRunItems findResultFor(String testCaseId) {
        if (currentTestRun == null || currentTestRun.getResults() == null) {
            return null;
        }

        // Convert the String ID to UUID for comparison
        try {
            UUID targetUuid = UUID.fromString(testCaseId);
            return currentTestRun.getResults().stream()
                    .filter(item -> item.getTestCaseId().equals(targetUuid))
                    .findFirst()
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void dispose() {
    }
}