package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import testGit.editorPanel.UnifiedVirtualFile;
import testGit.pojo.Config;
import testGit.pojo.dto.dirs.TestSetDirectoryDto;

import java.util.ArrayList;
import java.util.Optional;

public class TestEditor {

    public static void open(final TestSetDirectoryDto ts) {
        final UnifiedVirtualFile newVirtualFile = new UnifiedVirtualFile(ts, new ArrayList<>());

        ApplicationManager.getApplication().invokeLater(() ->
                Optional.ofNullable(FileEditorManager.getInstance(Config.getProject()))
                        .ifPresent(editorManager -> editorManager.openFile(newVirtualFile, true))
        );
    }
}