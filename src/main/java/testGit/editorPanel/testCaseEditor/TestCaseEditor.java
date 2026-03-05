package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import testGit.pojo.Config;
import testGit.pojo.Directory;
import testGit.pojo.TestCase;
import testGit.util.Notifier;

import java.io.File;
import java.util.*;

public class TestCaseEditor {

    public static void open(final Directory testSet) {
        FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());

        editorManager.openFile(
                Arrays.stream(editorManager.getOpenFiles())
                        .filter(f -> f instanceof VirtualFileImpl vf && vf.getDir().equals(testSet))
                        .findFirst()
                        .orElseGet(() -> createVirtualFile(testSet)),
                true
        );
    }

    private static VirtualFile createVirtualFile(Directory testSet) {
        List<TestCase> testCases = Optional.ofNullable(testSet.getFile())
                .filter(f -> f.exists() && f.isDirectory())
                .map(f -> f.listFiles((d, name) -> name.endsWith(".json")))
                .stream()
                .flatMap(Arrays::stream)
                //.parallel()
                .map(TestCaseEditor::addTestCase)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TestCase::getTitle))
                .toList();

        return new VirtualFileImpl(testSet, testCases);
    }

    private static TestCase addTestCase(File file) {
        try {
            return Config.getMapper().readValue(file, TestCase.class);
        } catch (Exception e) {
            Notifier.error("Read Test Case failed", e.getMessage());
            return null;
        }
    }
}