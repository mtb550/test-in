package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import testGit.pojo.*;
import testGit.projectPanel.ProjectPanel;
import testGit.util.DirectoryMapper;
import testGit.util.TestCaseSorter;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class TestRunEditor {

    public static void open(Path runFilePath, ProjectPanel projectPanel) {
        try {
            TestRun metadata = Config.getMapper().readValue(runFilePath.toFile(), TestRun.class);
            List<TestCase> testCases = loadTestCasesForRun(metadata, projectPanel);
            List<TestCase> sorted = TestCaseSorter.sortTestCases(testCases);

            VirtualFileImpl virtualFile = new VirtualFileImpl(
                    runFilePath.toAbsolutePath().toString(),
                    buildFilteredModel(sorted),
                    sorted,
                    EditorType.TEST_RUN_OPENING,
                    projectPanel
            );
            virtualFile.setMetadata(metadata);

            FileEditorManager.getInstance(Config.getProject()).openFile(virtualFile, true);
        } catch (IOException e) {
            System.err.println("Failed to open Test Run: " + e.getMessage());
        }
    }

    public static void create(Path runFilePath, ProjectPanel projectPanel, Directory projectName, TestRun metadata) {
        Path testCasesPath = Config.getTestGitPath().resolve(projectName.getFileName()).resolve("testCases");
        DefaultTreeModel fullModel = new DefaultTreeModel(buildDirectoryTree(testCasesPath.toFile(), true));

        VirtualFileImpl virtualFile = new VirtualFileImpl(
                runFilePath.toAbsolutePath().toString(),
                fullModel,
                new ArrayList<>(),
                EditorType.TEST_RUN_CREATION,
                projectPanel
        );
        virtualFile.setMetadata(metadata);

        FileEditorManager.getInstance(Config.getProject()).openFile(virtualFile, true);
    }

    // --- Private helpers ---

    private static List<TestCase> loadTestCasesForRun(TestRun metadata, ProjectPanel projectPanel) throws IOException {
        if (metadata.getResults() == null) return Collections.emptyList();

        Set<String> targetIds = metadata.getResults().stream()
                .map(item -> item.getTestCaseId().toString())
                .collect(Collectors.toSet());

        String projectName = projectPanel.getTestProjectSelector().getSelectedTestProject().getItem().getFileName();
        Path testCasesRoot = Config.getTestGitPath().resolve(projectName).resolve("testCases");

        if (!Files.exists(testCasesRoot)) return Collections.emptyList();

        List<TestCase> cases = new ArrayList<>();
        try (var paths = Files.walk(testCasesRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            TestCase tc = Config.getMapper().readValue(p.toFile(), TestCase.class);
                            if (tc.getId() != null && targetIds.contains(tc.getId())) {
                                cases.add(tc);
                            }
                        } catch (Exception ignored) {
                        }
                    });
        }
        return cases;
    }

    private static DefaultTreeModel buildFilteredModel(List<TestCase> cases) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Selected Test Cases");
        cases.forEach(tc -> root.add(new DefaultMutableTreeNode(tc)));
        return new DefaultTreeModel(root);
    }

    private static DefaultMutableTreeNode buildDirectoryTree(File folder, boolean isRoot) {
        Object label = isRoot
                ? "Test Cases (" + folder.getParentFile().getName() + ")"
                : Optional.ofNullable((Object) DirectoryMapper.map(folder)).orElse(folder.getName());

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(label);

        File[] children = folder.listFiles();
        if (children == null) return node;

        Arrays.sort(children, Comparator
                .comparing((File f) -> !f.isDirectory())
                .thenComparing(f -> f.getName().toLowerCase()));

        for (File child : children) {
            if (child.isDirectory()) {
                node.add(buildDirectoryTree(child, false));
            } else if (child.isFile() && child.getName().endsWith(".json")) {
                try {
                    TestCase tc = Config.getMapper().readValue(child, TestCase.class);
                    node.add(new DefaultMutableTreeNode(tc));
                } catch (Exception e) {
                    System.err.println("Failed to parse test case: " + child.getName());
                }
            }
        }
        return node;
    }
}
