package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import testGit.pojo.*;
import testGit.projectPanel.ProjectPanel;
import testGit.util.TestCaseSorter;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class TestRunEditor {

    private static DefaultTreeModel createFilteredModel(List<TestCase> cases) {
        System.out.println("TestRunEditor.createFilteredModel()");
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Selected Test Cases");
        for (TestCase tc : cases) {
            root.add(new DefaultMutableTreeNode(tc));
        }
        return new DefaultTreeModel(root);
    }

    public static void open(Path runFilePath, ProjectPanel projectPanel) {
        System.out.println("TestRunEditor.open()");
        FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());
        String targetPath = runFilePath.toAbsolutePath().toString();

        TestRun metadata;
        try {
            metadata = Config.getMapper().readValue(runFilePath.toFile(), TestRun.class);
        } catch (IOException e) {
            return;
        }

        List<TestCase> testCases = new ArrayList<>();
        if (metadata.getResults() != null) {
            for (TestRun.TestRunItems item : metadata.getResults()) {
                if (item.getProject() != null && item.getTestCaseId() != null) {
                    TestCase tc = findTestCaseRecursively(item.getProject(), item.getTestCaseId().toString());
                    if (tc != null) {
                        testCases.add(tc);
                    }
                }
            }
        }

        List<TestCase> sortedCases = TestCaseSorter.sortTestCases(testCases);
        openEditor(editorManager, targetPath, metadata, sortedCases, EditorType.TEST_RUN_OPENING, projectPanel);
    }

    public static void create(Path runPath, ProjectPanel projectPanel, DefaultMutableTreeNode selectedNode, TestRun metadata) {
        System.out.println("[DEBUG] TestRunEditor: creating editor for path: " + runPath);

        FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());
        // Normalize path to absolute string
        String targetPath = runPath.toAbsolutePath().toString();

        Directory testSet = (Directory) selectedNode.getUserObject();
        List<TestCase> testCases = new ArrayList<>();
        File folder = testSet.getFile();

        System.out.println("[DEBUG] TestRunEditor: Scanning folder: " + folder.getAbsolutePath());

        if (folder != null && folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((d, name) -> name.toLowerCase().endsWith(".json"));
            if (files != null) {
                System.out.println("[DEBUG] TestRunEditor: Found " + files.length + " JSON files.");
                for (File file : files) {
                    try {
                        TestCase tc = Config.getMapper().readValue(file, TestCase.class);
                        testCases.add(tc);
                        System.out.println("[DEBUG] TestRunEditor: Successfully parsed test case: " + tc.getTitle());
                    } catch (Exception e) {
                        System.err.println("[DEBUG] TestRunEditor: Failed to parse " + file.getName() + " - " + e.getMessage());
                    }
                }
            } else {
                System.out.println("[DEBUG] TestRunEditor: No files found in directory.");
            }
        }

        List<TestCase> sortedCases = TestCaseSorter.sortTestCases(testCases);
        System.out.println("[DEBUG] TestRunEditor: Total test cases ready: " + sortedCases.size());

        VirtualFile existingFile = Arrays.stream(editorManager.getOpenFiles())
                .filter(f -> f instanceof VirtualFileImpl && ((VirtualFileImpl) f).getRunPath().equals(targetPath))
                .findFirst()
                .orElse(null);

        if (existingFile != null) {
            editorManager.openFile(existingFile, true);
        } else {
            VirtualFileImpl virtualFile = new VirtualFileImpl(
                    targetPath,
                    (DefaultTreeModel) projectPanel.getTestCaseTabController().getTree().getModel(),
                    sortedCases,
                    EditorType.TEST_RUN_CREATION,
                    projectPanel
            );

            virtualFile.setMetadata(metadata);
            editorManager.openFile(virtualFile, true);
        }
    }

    private static void openEditor(FileEditorManager editorManager, String targetPath, TestRun metadata,
                                   List<TestCase> sortedCases, EditorType type, ProjectPanel projectPanel) {
        System.out.println("TestRunEditor.openEditor()");
        VirtualFile existingFile = Arrays.stream(editorManager.getOpenFiles())
                .filter(f -> f instanceof VirtualFileImpl && ((VirtualFileImpl) f).getRunPath().equals(targetPath))
                .findFirst()
                .orElse(null);

        if (existingFile != null) {
            editorManager.openFile(existingFile, true);
        } else {
            VirtualFileImpl virtualFile = new VirtualFileImpl(
                    targetPath,
                    createFilteredModel(sortedCases),
                    sortedCases,
                    type,
                    projectPanel
            );
            virtualFile.setMetadata(metadata);
            editorManager.openFile(virtualFile, true);
        }
    }

    public static TestCase findTestCaseRecursively(String projectName, String testCaseId) {
        if (projectName == null || testCaseId == null || Config.getTestGitPath() == null) {
            System.out.println("findTestCaseRecursively. projectName == null || testCaseId == null || Config.getTestGitPath() == null");
            return null;
        }

        Path searchRoot = Config.getTestGitPath()
                .resolve(projectName)
                .resolve("testCases");

        System.out.println("searchRoot: " + searchRoot);
        if (!Files.exists(searchRoot)) {
            return null;
        }

        try (Stream<Path> paths = Files.walk(searchRoot)) {
            Optional<Path> foundFile = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(testCaseId + ".json"))
                    .findFirst();

            if (foundFile.isPresent()) {
                return Config.getMapper().readValue(foundFile.get().toFile(), TestCase.class);
            }
        } catch (IOException ignored) {
        }
        return null;
    }
}