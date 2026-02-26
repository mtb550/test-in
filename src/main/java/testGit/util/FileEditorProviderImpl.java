package testGit.util;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.testRunEditor.FileEditorCreationImpl;
import testGit.editorPanel.testRunEditor.FileEditorOpeningImpl;
import testGit.editorPanel.testRunEditor.VirtualFileImpl;

public class FileEditorProviderImpl implements FileEditorProvider {
    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return file instanceof VirtualFileImpl;
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        VirtualFileImpl vf = (VirtualFileImpl) file;

        return switch (vf.getEditorType()) {
            case TEST_RUN_CREATION -> new FileEditorCreationImpl(vf);
            case TEST_RUN_OPENING -> new FileEditorOpeningImpl(vf);
            case TEST_SET_OPEN -> null;
        };
    }

    @Override
    public @NotNull String getEditorTypeId() {
        return "TestRunCreationEditor";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}