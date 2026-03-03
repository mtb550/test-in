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
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Selected Test Cases");
        for (TestCase tc : cases) {
            root.add(new DefaultMutableTreeNode(tc));
        }
        return new DefaultTreeModel(root);
    }

    public static void open(Path runFilePath, ProjectPanel projectPanel) {
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
        FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());
        String targetPath = runPath.toAbsolutePath().toString();

        Directory testSet = (Directory) selectedNode.getUserObject();
        List<TestCase> testCases = new ArrayList<>();
        File folder = testSet.getFile();

        if (folder != null && folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((d, name) -> name.toLowerCase().endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try {
                        TestCase tc = Config.getMapper().readValue(file, TestCase.class);
                        testCases.add(tc);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        List<TestCase> sortedCases = TestCaseSorter.sortTestCases(testCases);
        openEditor(editorManager, targetPath, metadata, sortedCases, EditorType.TEST_RUN_CREATION, projectPanel);
    }

    private static void openEditor(FileEditorManager editorManager, String targetPath, TestRun metadata,
                                   List<TestCase> sortedCases, EditorType type, ProjectPanel projectPanel) {

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
            return null;
        }

        Path searchRoot = Config.getTestGitPath()
                .resolve(projectName)
                .resolve("testCases");

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