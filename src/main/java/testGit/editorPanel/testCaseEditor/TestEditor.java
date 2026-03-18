package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import testGit.editorPanel.UnifiedVirtualFile;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.dirs.TestSetDirectoryDto;
import testGit.util.Notifier;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TestEditor {

    public static void open(final TestSetDirectoryDto ts) {
        FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());

        VirtualFile newVirtualFile = createVirtualFile(ts);
        editorManager.openFile(newVirtualFile, true);
    }

    private static VirtualFile createVirtualFile(TestSetDirectoryDto testSetDirectory) {
        List<TestCaseDto> testCaseDtos = Optional.of(testSetDirectory.getPath().toFile())
                .filter(f -> f.exists() && f.isDirectory())
                .map(f -> f.listFiles((d, name) -> name.endsWith(".json")))
                .stream()
                .flatMap(Arrays::stream)
                .map(TestEditor::addTestCase)
                .filter(Objects::nonNull)
                .toList();

        return new UnifiedVirtualFile(testSetDirectory, testCaseDtos);
    }

    private static TestCaseDto addTestCase(File file) {
        try {
            return Config.getMapper().readValue(file, TestCaseDto.class);
        } catch (Exception e) {
            Notifier.error("Read Test Case failed", e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }
}