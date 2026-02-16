package testGit.editorPanel.testCaseEditor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import testGit.pojo.Config;
import testGit.pojo.Directory;
import testGit.pojo.TestCase;
import testGit.util.TestCaseSorter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TestCaseEditor {
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public static void open(final Directory dir) {
        FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());
        List<TestCase> testCases = new ArrayList<>();
        File folder = dir.getFile();

        if (folder != null && folder.exists() && folder.isDirectory()) {
            Optional.ofNullable(folder.listFiles((d, name) -> name.toLowerCase().endsWith(".json")))
                    .ifPresent(files -> Arrays.stream(files).forEach(file -> {
                        try {
                            testCases.add(mapper.readValue(file, TestCase.class));
                        } catch (Exception ignored) {
                        }
                    }));
        }

        List<TestCase> sortedCases = TestCaseSorter.sortTestCases(testCases);

        VirtualFile existing = Arrays.stream(editorManager.getOpenFiles())
                .filter(f -> f instanceof VirtualFileImpl vf && vf.getDir().equals(dir))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            editorManager.openFile(existing, true);
        } else {
            editorManager.openFile(new VirtualFileImpl(dir, sortedCases), true);
        }
    }
}