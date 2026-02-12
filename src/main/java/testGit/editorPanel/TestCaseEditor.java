package testGit.editorPanel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import testGit.demo.TestCaseVirtualFile;
import testGit.pojo.Config;
import testGit.pojo.TestCase;
import testGit.util.TestCaseSorter;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TestCaseEditor {

    public static void open(final Path featurePath) {
        System.out.println("TestCaseEditor.open() , path: " + featurePath);

        FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        List<TestCase> testCases = new ArrayList<>();
        File folder = featurePath.toFile();

        // Load files if the directory exists
        if (folder.exists() && folder.isDirectory()) {
            File[] jsonFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));

            if (jsonFiles != null) {
                for (File file : jsonFiles) {
                    try {
                        TestCase tc = mapper.readValue(file, TestCase.class);
                        testCases.add(tc);
                    } catch (Exception e) {
                        System.err.println("Error parsing file: " + file.getName() + " -> " + e.getMessage());
                    }
                }
            }
        }

        // Sort if we found any, otherwise it just stays an empty list
        //testCases.sort(Comparator.comparingInt(TestCase::getSort));
        testCases = TestCaseSorter.sortTestCases(testCases);

        // 1. Check if a tab for this path is already open
        for (VirtualFile openFile : editorManager.getOpenFiles()) {
            if (openFile instanceof TestCaseVirtualFile existing && existing.getFeaturePath().equals(featurePath.toString())) {
                System.out.println("open test set: " + existing.getFeaturePath());
                editorManager.openFile(existing, true);
                return;
            }
        }

        VirtualFile virtualFile = new TestCaseVirtualFile(featurePath.toString(), testCases);
        editorManager.openFile(virtualFile, true);
    }
}
