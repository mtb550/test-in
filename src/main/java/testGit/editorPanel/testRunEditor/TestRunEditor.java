package testGit.editorPanel.testRunEditor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import testGit.pojo.Config;
import testGit.pojo.Directory;
import testGit.pojo.TestCase;
import testGit.projectPanel.ProjectPanel;
import testGit.util.TestCaseSorter;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestRunEditor {
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public static void open(final Path runPath, ProjectPanel projectPanel, DefaultMutableTreeNode selectedNode) {
        System.out.println("[DEBUG] TestRunEditor: Opening editor for path: " + runPath);

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
                        TestCase tc = mapper.readValue(file, TestCase.class);
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
                    (DefaultTreeModel) projectPanel.getTestCaseTree().getModel(),
                    sortedCases
            );
            editorManager.openFile(virtualFile, true);
        }
    }
}