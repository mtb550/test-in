package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.SneakyThrows;
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

    @SneakyThrows
    public static void open(final Path runFilePath) {
        System.out.println("[DEBUG] TestRunEditor: Opening editor for path: " + runFilePath);

        FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());
        String targetPath = runFilePath.toAbsolutePath().toString();

        TestRun metadata = Config.getMapper().readValue(runFilePath.toFile(), TestRun.class);
        // 1. Resolve Test Cases using the Metadata Results instead of folder scanning
        List<TestCase> testCases = new ArrayList<>();

        if (metadata.getResults() != null) {
            for (TestRun.TestRunItems item : metadata.getResults()) {
                TestCase tc = findTestCaseRecursively(item.getProject(), item.getTestCaseId().toString());
                if (tc != null) {
                    testCases.add(tc);
                } else {
                    System.err.println("[DEBUG] TestRunEditor: Could not find case: " + item.getTestCaseId());
                }
            }
        }

        // 2. Sort the retrieved cases
        List<TestCase> sortedCases = TestCaseSorter.sortTestCases(testCases);
        System.out.println("[DEBUG] TestRunEditor: Total test cases ready: " + sortedCases.size());

        // 3. Open the file via your virtual file system
        VirtualFile existingFile = Arrays.stream(editorManager.getOpenFiles())
                .filter(f -> f instanceof VirtualFileImpl && ((VirtualFileImpl) f).getRunPath().equals(targetPath))
                .findFirst()
                .orElse(null);

        if (existingFile != null) {
            editorManager.openFile(existingFile, true);
        } else {
            VirtualFileImpl virtualFile = new VirtualFileImpl(
                    targetPath,
                    createFilteredModel(sortedCases), // Pass the restricted model
                    sortedCases,
                    EditorType.TEST_RUN_OPENING
            );

            virtualFile.setMetadata(metadata);
            editorManager.openFile(virtualFile, true);
        }
    }

    // Helper to create a restricted model
    private static DefaultTreeModel createFilteredModel(List<TestCase> cases) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Selected Test Cases");
        for (TestCase tc : cases) {
            root.add(new DefaultMutableTreeNode(tc));
        }
        return new DefaultTreeModel(root);
    }

    public static void create(final Path runPath, ProjectPanel projectPanel, DefaultMutableTreeNode selectedNode, TestRun metadata) {
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
                    (DefaultTreeModel) ProjectPanel.testCaseTree.getModel(),
                    sortedCases,
                    EditorType.TEST_RUN_CREATION
            );

            virtualFile.setMetadata(metadata);
            editorManager.openFile(virtualFile, true);
        }
    }

    public static TestCase findTestCaseRecursively(String projectName, String testCaseId) {
        Path searchRoot = Config.getRootFolderFile().toPath()
                .resolve(projectName)
                .resolve("testCases");

        if (!Files.exists(searchRoot)) return null;

        try (Stream<Path> paths = Files.walk(searchRoot)) {
            Optional<Path> foundFile = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(testCaseId + ".json"))
                    .findFirst();

            if (foundFile.isPresent()) {
                System.out.println("find the file: path: " + foundFile.get().toFile().toPath());
                return Config.getMapper().readValue(foundFile.get().toFile(), TestCase.class);
            }
        } catch (IOException e) {
            System.err.println("Error searching for file: " + e.getMessage());
        }
        return null;
    }
}