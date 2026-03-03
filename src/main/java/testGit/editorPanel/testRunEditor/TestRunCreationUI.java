package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.*;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.*;
import testGit.projectPanel.ProjectPanel;
import testGit.util.TestCaseSorter;
import testGit.util.TestRunsDirectoryMapper;

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
@Setter
public class TestRunCreationUI implements Disposable {
    private final List<TestCase> initialTestCases;
    private CheckboxTree checklistTree;
    private TestRun currentTestRun;
    private TestRun metadata;
    private VirtualFile currentFile;

    public TestRunCreationUI(List<TestCase> initialTestCases) {
        System.out.println("TestRunCreationUI");
        this.initialTestCases = TestCaseSorter.sortTestCases(initialTestCases);
    }

    public JComponent createEditorPanel(DefaultTreeModel testCaseModel, String savePathString, ProjectPanel projectPanel) {
        JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());

        CheckedTreeNode root = convertToCheckedNodes((DefaultMutableTreeNode) testCaseModel.getRoot());

        checklistTree = new CheckboxTree(
                new CheckboxTree.CheckboxTreeCellRenderer() {
                    @Override
                    public void customizeRenderer(@NotNull JTree tree, @NotNull Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                        if (value instanceof CheckedTreeNode node) {
                            Object userObj = node.getUserObject();
                            if (userObj instanceof Directory dir) {
                                getTextRenderer().append(dir.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                            } else if (userObj instanceof TestCase tc) {
                                TestRun.TestRunItems result = findResultFor(tc.getId());

                                SimpleTextAttributes mainStyle = SimpleTextAttributes.REGULAR_ATTRIBUTES;
                                String statusText = " [Pending]";

                                if (result != null) {
                                    switch (result.getStatus()) {
                                        case "PASSED" -> {
                                            mainStyle = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.BLUE);
                                            statusText = " [Passed]";
                                        }
                                        case "FAILED" -> {
                                            mainStyle = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.RED);
                                            statusText = " [Failed]";
                                        }
                                        case "BLOCKED" -> {
                                            mainStyle = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.ORANGE);
                                            statusText = " [Blocked]";
                                        }
                                    }
                                }

                                getTextRenderer().append(tc.getTitle(), mainStyle);
                                getTextRenderer().append(statusText, SimpleTextAttributes.GRAYED_ATTRIBUTES);
                            }
                        }
                    }
                },
                root,
                new CheckboxTreeBase.CheckPolicy(true, true, true, true)
        );

        TreeUtil.expandAll(checklistTree);

        mainPanel.add(new JBScrollPane(checklistTree), BorderLayout.CENTER);

        JButton saveButton = new JButton("Save Test Run");
        saveButton.addActionListener(e -> saveSelectedToJSON(root, savePathString, projectPanel));
        mainPanel.add(saveButton, BorderLayout.SOUTH);

        return mainPanel;
    }

    private CheckedTreeNode convertToCheckedNodes(DefaultMutableTreeNode node) {
        Object userObj = node.getUserObject();
        CheckedTreeNode newNode = new CheckedTreeNode(userObj);

        for (int i = 0; i < node.getChildCount(); i++) {
            newNode.add(convertToCheckedNodes((DefaultMutableTreeNode) node.getChildAt(i)));
        }

        if (userObj instanceof Directory dir && dir.getType() == DirectoryType.TS) {
            List<TestCase> cases = loadTestCasesFromDir(dir);
            for (TestCase tc : cases) {
                CheckedTreeNode tcNode = new CheckedTreeNode(tc);

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
                        testCases.add(Config.getMapper().readValue(file, TestCase.class));
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return TestCaseSorter.sortTestCases(testCases);
    }

    private void saveSelectedToJSON(CheckedTreeNode root, String baseProjectPath, ProjectPanel projectPanel) {
        File testRunsDir = new File(baseProjectPath, "testRuns");

        TestRun run = this.currentTestRun != null ? this.currentTestRun : new TestRun();

        if (this.metadata != null) {
            run.setBuildNumber(metadata.getBuildNumber());
            run.setPlatform(metadata.getPlatform());
            run.setLanguage(metadata.getLanguage());
            run.setBrowser(metadata.getBrowser());
            run.setDeviceType(metadata.getDeviceType());
        }

        assert metadata != null;
        String fileName = "tr_" + metadata.getBuildNumber() + "_1.json";
        File finalOutputFile = new File(testRunsDir, fileName);

        run.setRunName(fileName);
        run.setCreatedAt(LocalDateTime.now());
        run.setStatus(TestRunStatus.CREATED);

        List<TestRun.TestRunItems> items = new ArrayList<>();
        collectCheckedItems(root, items);
        run.setResults(items);

        try {
            Config.getMapper().writerWithDefaultPrettyPrinter().writeValue(finalOutputFile, run);
            System.out.println("Test Run saved successfully to: " + finalOutputFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        TestRunsDirectoryMapper.buildTreeAsync(projectPanel.getTestRunTree());

        if (currentFile != null) {
            ApplicationManager.getApplication().invokeLater(() ->
                    FileEditorManager.getInstance(Config.getProject())
                            .closeFile(currentFile));
        }


    }

    private void collectCheckedItems(CheckedTreeNode node, List<TestRun.TestRunItems> items) {
        if (node.getUserObject() instanceof TestCase tc && node.isChecked()) {
            TestRun.TestRunItems item = new TestRun.TestRunItems();
            item.setTestCaseId(UUID.fromString(tc.getId()));
            item.setStatus("PENDING");

            Object rootObject = ((DefaultMutableTreeNode) node.getRoot()).getUserObject();
            if (rootObject instanceof Directory rootDir) {
                item.setProject(rootDir.getFileName());
            }
            items.add(item);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            collectCheckedItems((CheckedTreeNode) node.getChildAt(i), items);
        }
    }

    private TestRun.TestRunItems findResultFor(String testCaseId) {
        if (currentTestRun == null || currentTestRun.getResults() == null) {
            return null;
        }

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