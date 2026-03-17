package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import testGit.pojo.Config;
import testGit.pojo.EditorType;
import testGit.pojo.TestProject;
import testGit.pojo.TestRun;
import testGit.pojo.mappers.TestCaseJsonMapper;
import testGit.pojo.mappers.TestRunJsonMapper;
import testGit.pojo.mappers.TestSetMapper;
import testGit.projectPanel.ProjectPanel;
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

    public static void open(TestRun pkg, ProjectPanel projectPanel) {
        try {
            TestRunJsonMapper metadata = Config.getMapper().readValue(pkg.getPath().toFile(), TestRunJsonMapper.class);
            List<TestCaseJsonMapper> testCaseJsonMappers = loadTestCasesForRun(metadata, projectPanel);
            List<TestCaseJsonMapper> sorted = TestCaseSorter.sortTestCases(testCaseJsonMappers);

            VirtualFileImpl virtualFile = new VirtualFileImpl(
                    pkg,
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

    public static void create(TestRun pkg, ProjectPanel projectPanel, TestProject testProjectName, TestRunJsonMapper metadata) {
        Path testCasesPath = Config.getTestGitPath().resolve(testProjectName.getPathName()).resolve("testCases");
        DefaultTreeModel fullModel = new DefaultTreeModel(buildDirectoryTree(testCasesPath.toFile(), true));

        VirtualFileImpl virtualFile = new VirtualFileImpl(
                pkg,
                fullModel,
                new ArrayList<>(),
                EditorType.TEST_RUN_CREATION,
                projectPanel
        );
        virtualFile.setMetadata(metadata);

        FileEditorManager.getInstance(Config.getProject()).openFile(virtualFile, true);
    }

    // --- Private helpers ---

    private static List<TestCaseJsonMapper> loadTestCasesForRun(TestRunJsonMapper metadata, ProjectPanel projectPanel) throws IOException {
        if (metadata.getResults() == null) return Collections.emptyList();

        Set<String> targetIds = metadata.getResults().stream()
                .map(item -> item.getTestCaseId().toString())
                .collect(Collectors.toSet());

        String projectName = projectPanel.getTestProjectSelector().getSelectedTestProject().getItem().getPathName();
        Path testCasesRoot = Config.getTestGitPath().resolve(projectName).resolve("testCases");

        if (!Files.exists(testCasesRoot)) return Collections.emptyList();

        List<TestCaseJsonMapper> cases = new ArrayList<>();
        try (var paths = Files.walk(testCasesRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            TestCaseJsonMapper tc = Config.getMapper().readValue(p.toFile(), TestCaseJsonMapper.class);
                            if (tc.getId() != null && targetIds.contains(tc.getId())) {
                                cases.add(tc);
                            }
                        } catch (Exception ignored) {
                        }
                    });
        }
        return cases;
    }

    private static DefaultTreeModel buildFilteredModel(List<TestCaseJsonMapper> cases) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Selected Test Cases");
        cases.forEach(tc -> root.add(new DefaultMutableTreeNode(tc)));
        return new DefaultTreeModel(root);
    }

    private static DefaultMutableTreeNode buildDirectoryTree(File folder, boolean isRoot) {
        Object label = isRoot
                ? "Test Cases (" + folder.getParentFile().getName() + ")"
                : Optional.ofNullable((Object) TestSetMapper.map(folder.toPath())).orElse(folder.getName()); // to be updated

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
                    TestCaseJsonMapper tc = Config.getMapper().readValue(child, TestCaseJsonMapper.class);
                    node.add(new DefaultMutableTreeNode(tc));
                } catch (Exception e) {
                    System.err.println("Failed to parse test case: " + child.getName());
                }
            }
        }
        return node;
    }
}
