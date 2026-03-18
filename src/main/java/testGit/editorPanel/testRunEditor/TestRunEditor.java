package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import testGit.pojo.Config;
import testGit.pojo.EditorType;
import testGit.pojo.TestProject;
import testGit.pojo.TestRun;
import testGit.pojo.mappers.TestCaseJsonMapper;
import testGit.pojo.mappers.TestRunJsonMapper;
import testGit.pojo.mappers.TestSetMapper;
import testGit.pojo.mappers.TestSetPackageMapper;
import testGit.projectPanel.ProjectPanel;
import testGit.util.TestCaseSorter;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static void create(TestRun tr, ProjectPanel projectPanel, TestProject tp, TestRunJsonMapper metadata) {
        Path testCasesPath = tp.getTestCasesDirectory().getPath();

        DefaultTreeModel fullModel = new DefaultTreeModel(buildDirectoryTree(testCasesPath, true));

        VirtualFileImpl virtualFile = new VirtualFileImpl(
                tr,
                fullModel,
                new ArrayList<>(),
                EditorType.TEST_RUN_CREATION,
                projectPanel
        );
        virtualFile.setMetadata(metadata);

        FileEditorManager.getInstance(Config.getProject()).openFile(virtualFile, true);
    }

    // --- Private helpers ---

    private static List<TestCaseJsonMapper> loadTestCasesForRun(TestRunJsonMapper metadata, ProjectPanel projectPanel) {
        if (metadata.getResults() == null) return Collections.emptyList();

        Set<String> targetIds = metadata.getResults().stream()
                .map(item -> item.getTestCaseId().toString())
                .collect(Collectors.toSet());

        Path testCasesRoot = projectPanel.getTestProjectSelector().getSelectedTestProject().getItem().getTestCasesDirectory().getPath();

        if (!Files.exists(testCasesRoot)) return Collections.emptyList();

        List<TestCaseJsonMapper> cases = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(testCasesRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            TestCaseJsonMapper tc = Config.getMapper().readValue(p.toFile(), TestCaseJsonMapper.class);
                            if (tc.getId() != null && targetIds.contains(tc.getId())) {
                                cases.add(tc);
                            }
                        } catch (Exception ignored) {
                            // تجاهل الملفات غير الصالحة بصمت
                        }
                    });
        } catch (IOException e) {
            System.err.println("Failed to load test cases for run: " + e.getMessage());
        }

        return cases;
    }

    private static DefaultTreeModel buildFilteredModel(List<TestCaseJsonMapper> cases) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Selected Test Cases");
        cases.forEach(tc -> root.add(new DefaultMutableTreeNode(tc)));
        return new DefaultTreeModel(root);
    }

    private static DefaultMutableTreeNode buildDirectoryTree(Path folder, boolean isRoot) {
        Object label = isRoot
                ? "Test Cases (" + folder.getParent().getFileName().toString() + ")"
                : resolveDirectoryObject(folder); // 🌟 استخدام دالة مساعدة ذكية للمابرز

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(label);

        if (!Files.exists(folder) || !Files.isDirectory(folder)) return node;

        try (Stream<Path> paths = Files.list(folder)) {
            List<Path> sortedPaths = paths.sorted(Comparator
                            .comparing((Path p) -> !Files.isDirectory(p))
                            .thenComparing(p -> p.getFileName().toString().toLowerCase()))
                    .toList();

            for (Path child : sortedPaths) {
                if (Files.isDirectory(child)) {
                    node.add(buildDirectoryTree(child, false));
                } else if (child.toString().endsWith(".json")) {
                    try {
                        TestCaseJsonMapper tc = Config.getMapper().readValue(child.toFile(), TestCaseJsonMapper.class);
                        node.add(new DefaultMutableTreeNode(tc));
                    } catch (Exception e) {
                        System.err.println("Failed to parse test case: " + child.getFileName());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read directory tree: " + folder);
            e.printStackTrace(System.err);
        }

        return node;
    }

    private static Object resolveDirectoryObject(Path folder) {
        if (Files.exists(folder.resolve(".tcp"))) return TestSetPackageMapper.map(folder);
        if (Files.exists(folder.resolve(".ts"))) return TestSetMapper.map(folder);

        return folder.getFileName().toString();
    }
}