package org.testin.editorPanel;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.runEditor.RunEditor;
import org.testin.editorPanel.testEditor.TestEditor;

public class UnifiedEditorProvider implements FileEditorProvider, DumbAware {

    @Override
    public boolean accept(final @NotNull Project p, final @NotNull VirtualFile file) {
        return file instanceof UnifiedVirtualFile vf && vf.isValid();
    }

    @Override
    public @NotNull FileEditor createEditor(final @NotNull Project p, final @NotNull VirtualFile file) {
        if (file instanceof UnifiedVirtualFile unifiedFile) {

            final IEditor editor;

            if (unifiedFile.getFileType() == EditorType.TEST_RUN)
                editor = new RunEditor(p, unifiedFile);

            else if (unifiedFile.getFileType() == EditorType.TEST_CASE)
                editor = new TestEditor(p, unifiedFile);

            else
                throw new IllegalArgumentException("Unknown FileType: " + unifiedFile.getFileType());

            return new UnifiedFileEditor(p, unifiedFile, editor);
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