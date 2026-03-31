package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.application.ApplicationManager;
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
        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            VirtualFile newVirtualFile = createVirtualFile(ts);

            ApplicationManager.getApplication().invokeLater(() -> {
                FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());
                editorManager.openFile(newVirtualFile, true);
            });
        });
    }

    private static VirtualFile createVirtualFile(final TestSetDirectoryDto testSetDirectory) {

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

    private static TestCaseDto addTestCase(final File file) {
        try {

            return Config.getMapper().readValue(file, TestCaseDto.class);

        } catch (Exception e) {
            Notifier.error("Read Test Case failed", file.getName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }
}