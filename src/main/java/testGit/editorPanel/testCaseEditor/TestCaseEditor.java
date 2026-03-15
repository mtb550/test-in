package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import testGit.pojo.Config;
import testGit.pojo.TestCase;
import testGit.pojo.TestPackage;
import testGit.util.Notifier;

import java.io.File;
import java.util.*;

public class TestCaseEditor {

    public static void open(final TestPackage testSet) {
        FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());

        editorManager.openFile(
                Arrays.stream(editorManager.getOpenFiles())
                        .filter(f -> f instanceof VirtualFileImpl vf && vf.getPkg().equals(testSet))
                        .findFirst()
                        .orElseGet(() -> createVirtualFile(testSet)),
                true
        );
    }

    private static VirtualFile createVirtualFile(TestPackage testSet) {
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