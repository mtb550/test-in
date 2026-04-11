package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import testGit.editorPanel.UnifiedVirtualFile;
import testGit.pojo.Config;
import testGit.pojo.dto.dirs.TestSetDirectoryDto;

import java.util.ArrayList;

public class TestEditor {

    public static void open(final TestSetDirectoryDto ts) {
        UnifiedVirtualFile newVirtualFile = new UnifiedVirtualFile(ts, new ArrayList<>());

        ApplicationManager.getApplication().invokeLater(() -> {
            FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());
            editorManager.openFile(newVirtualFile, true);
        });
    }

}