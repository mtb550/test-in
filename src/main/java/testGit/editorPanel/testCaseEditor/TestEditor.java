package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import testGit.editorPanel.UnifiedVirtualFile;
import testGit.pojo.Config;
import testGit.pojo.mappers.TestCase;
import testGit.pojo.tree.dirs.TestSetDirectory;
import testGit.util.Notifier;

import java.io.File;
import java.util.*;

public class TestEditor {

    public static void open(final TestSetDirectory ts) {
        FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());

        VirtualFile newVirtualFile = createVirtualFile(ts);
        editorManager.openFile(newVirtualFile, true);
    }

    private static VirtualFile createVirtualFile(TestSetDirectory testSetDirectory) {
        List<TestCase> testCases = Optional.of(testSetDirectory.getPath().toFile())
                .filter(f -> f.exists() && f.isDirectory())
                .map(f -> f.listFiles((d, name) -> name.endsWith(".json")))
                .stream()
                .flatMap(Arrays::stream)
                .map(TestEditor::addTestCase)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TestCase::getTitle))
                .toList();

        return new UnifiedVirtualFile(testSetDirectory, testCases);
    }

    private static TestCase addTestCase(File file) {
        try {
            return Config.getMapper().readValue(file, TestCase.class);
        } catch (Exception e) {
            Notifier.error("Read Test Case failed", e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }
}