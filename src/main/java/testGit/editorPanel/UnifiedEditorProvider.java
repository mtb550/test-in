package testGit.editorPanel;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.testCaseEditor.TestEditorUI;
import testGit.editorPanel.testRunEditor.RunEditorUI;

public class UnifiedEditorProvider implements FileEditorProvider, DumbAware {

    @Override
    public boolean accept(final @NotNull Project project, final @NotNull VirtualFile file) {
        return file instanceof UnifiedVirtualFile vf && vf.isValid();
    }

    @Override
    public @NotNull FileEditor createEditor(final @NotNull Project project, final @NotNull VirtualFile file) {
        if (file instanceof UnifiedVirtualFile unifiedFile) {

            BaseEditorUI ui;

            if (unifiedFile.getFileType() == FileType.TEST_RUN)
                ui = new RunEditorUI(unifiedFile);

            else if (unifiedFile.getFileType() == FileType.TEST_CASE)
                ui = new TestEditorUI(unifiedFile);

            else
                throw new IllegalArgumentException("Unknown FileType: " + unifiedFile.getFileType());

            return new UnifiedFileEditor(unifiedFile, ui);
        }

        throw new IllegalArgumentException("Unsupported virtual file type: " + file.getClass().getName());
    }

    @Override
    public @NotNull String getEditorTypeId() {
        return "test-git-unified-editor";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}