package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import testGit.pojo.Config;
import testGit.pojo.TestSet;
import testGit.pojo.mappers.TestCaseJsonMapper;
import testGit.util.Notifier;

import java.io.File;
import java.util.*;

public class TestCaseEditor {

    public static void open(final TestSet testSet) {
        FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());

        VirtualFile newVirtualFile = createVirtualFile(testSet);
        editorManager.openFile(newVirtualFile, true);
    }

    private static VirtualFile createVirtualFile(TestSet testSet) {
        List<TestCaseJsonMapper> testCaseJsonMappers = Optional.of(testSet.getPath().toFile())
                .filter(f -> f.exists() && f.isDirectory())
                .map(f -> f.listFiles((d, name) -> name.endsWith(".json")))
                .stream()
                .flatMap(Arrays::stream)
                //.parallel()
                .map(TestCaseEditor::addTestCase)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TestCaseJsonMapper::getTitle))
                .toList();

        return new VirtualFileImpl(testSet, testCaseJsonMappers);
    }

    private static TestCaseJsonMapper addTestCase(File file) {
        try {
            return Config.getMapper().readValue(file, TestCaseJsonMapper.class);
        } catch (Exception e) {
            Notifier.error("Read Test Case failed", e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }
}