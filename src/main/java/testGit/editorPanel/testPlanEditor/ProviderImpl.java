package testGit.editorPanel.testPlanEditor;

import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ProviderImpl implements FileEditorProvider {
    @Override
    public boolean accept(@NotNull Project project, @NotNull com.intellij.openapi.vfs.VirtualFile file) {
        return file instanceof VirtualFileImpl;
    }

    @Override
    public @NotNull com.intellij.openapi.fileEditor.FileEditor createEditor(@NotNull Project project, @NotNull com.intellij.openapi.vfs.VirtualFile file) {
        return new FileEditorImpl((VirtualFileImpl) file);
    }

    @Override
    public @NotNull String getEditorTypeId() {
        return "test-plan-checklist-editor";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}