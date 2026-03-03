package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class FileEditorProviderImpl implements FileEditorProvider, DumbAware {
    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return file instanceof VirtualFileImpl && file.isValid();
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        VirtualFileImpl vf = (VirtualFileImpl) file;

        return switch (vf.getEditorType()) {
            case TEST_RUN_CREATION -> new FileEditorCreationImpl(vf);
            case TEST_RUN_OPENING -> new FileEditorOpeningImpl(vf);
            case TEST_SET_OPEN -> throw new RuntimeException();
        };
    }

    @Override
    public @NotNull String getEditorTypeId() {
        return "test-run-editor";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}