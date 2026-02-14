package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class Provider implements FileEditorProvider, DumbAware {
    @Override
    public boolean accept(@NotNull Project project, @NotNull com.intellij.openapi.vfs.VirtualFile file) {
        System.out.println("EditorProvider.accept()");
        return file instanceof VirtualFile;
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull com.intellij.openapi.vfs.VirtualFile file) {
        System.out.println("EditorProvider.createEditor()");
        return new EditorPanel(((VirtualFile) file).getTestCases(), ((VirtualFile) file).getFeaturePath(), file);
    }

    @Override
    public @NotNull String getEditorTypeId() {
        //System.out.println("EditorProvider.getEditorTypeId()");
        return "test-case-editor";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        System.out.println("EditorProvider.getPolicy()");
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
