package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import testGit.editorPanel.UnifiedVirtualFile;
import testGit.pojo.Config;
import testGit.pojo.DirectoryMapper;
import testGit.pojo.DirectoryType;
import testGit.pojo.EditorType;
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.TestRunDto;
import testGit.pojo.dto.dirs.TestProjectDirectoryDto;
import testGit.pojo.dto.dirs.TestRunDirectoryDto;
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

public class RunEditor {

    public static void open(TestRunDirectoryDto tr, ProjectPanel projectPanel) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path jsonFilePath = tr.getPath().resolve(tr.getName() + ".json");

                if (!Files.exists(jsonFilePath)) {
                    System.err.println("JSON file not found: " + jsonFilePath);
                    return;
                }

                TestRunDto metadata = Config.getMapper().readValue(jsonFilePath.toFile(), TestRunDto.class);
                List<TestCaseDto> testCaseDtos = loadTestCasesForRun(metadata, projectPanel);
                List<TestCaseDto> sorted = TestCaseSorter.sortTestCases(testCaseDtos);

                UnifiedVirtualFile virtualFile = new UnifiedVirtualFile(
                        tr,
                        buildFilteredModel(sorted),
                        sorted,
                        EditorType.TEST_RUN_OPENING,
                        projectPanel
                );
                virtualFile.setMetadata(metadata);

                ApplicationManager.getApplication().invokeLater(() ->
                        FileEditorManager.getInstance(Config.getProject()).openFile(virtualFile, true));

            } catch (IOException e) {
                System.err.println("Failed to open Test Run: " + e.getMessage());
            }
        });
    }

    public static void create(TestRunDirectoryDto tr, ProjectPanel projectPanel, TestProjectDirectoryDto tp, TestRunDto metadata) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Path testCasesPath = tp.getTestCasesDirectory().getPath();

            DefaultTreeModel fullModel = new DefaultTreeModel(buildDirectoryTree(testCasesPath, true));

            UnifiedVirtualFile virtualFile = new UnifiedVirtualFile(
                    tr,
                    fullModel,
                    new ArrayList<>(),
                    EditorType.TEST_RUN_CREATION,
                    projectPanel
            );
            virtualFile.setMetadata(metadata);

            ApplicationManager.getApplication().invokeLater(() ->
                    FileEditorManager.getInstance(Config.getProject()).openFile(virtualFile, true));
        });
    }

    private static List<TestCaseDto> loadTestCasesForRun(TestRunDto metadata, ProjectPanel projectPanel) {
        if (metadata.getResults() == null) return Collections.emptyList();

        Set<UUID> targetIds = metadata.getResults().stream()
                .map(TestRunDto.TestRunItems::getTestCaseId)
                .collect(Collectors.toSet());

        Path testCasesRoot = projectPanel.getTestProjectSelector().getSelectedTestProject().getItem().getTestCasesDirectory().getPath();

        if (!Files.exists(testCasesRoot)) return Collections.emptyList();

        List<TestCaseDto> cases = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(testCasesRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            TestCaseDto tc = Config.getMapper().readValue(p.toFile(), TestCaseDto.class);
                            if (tc.getId() != null && targetIds.contains(tc.getId())) {
                                cases.add(tc);
                            }
                        } catch (Exception ignored) {
                        }
                    });
        } catch (IOException e) {
            System.err.println("Failed to load test cases for run: " + e.getMessage());
        }

        return cases;
    }

    private static DefaultTreeModel buildFilteredModel(List<TestCaseDto> cases) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Selected Test Cases");
        cases.forEach(tc -> root.add(new DefaultMutableTreeNode(tc)));
        return new DefaultTreeModel(root);
    }

    private static DefaultMutableTreeNode buildDirectoryTree(Path folder, boolean isRoot) {
        Object label = isRoot
                ? "Test Cases (" + folder.getParent().getFileName().toString() + ")"
                : resolveDirectoryObject(folder);

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
                        TestCaseDto tc = Config.getMapper().readValue(child.toFile(), TestCaseDto.class);
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
        if (Files.exists(folder.resolve(DirectoryType.TSP.getMarker())))
            return DirectoryMapper.testSetPackageNode(folder);
        if (Files.exists(folder.resolve(DirectoryType.TS.getMarker()))) return DirectoryMapper.testSetNode(folder);

        return folder.getFileName().toString();
    }
}