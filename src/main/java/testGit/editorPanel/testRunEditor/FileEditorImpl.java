package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;

public class FileEditorImpl extends UserDataHolderBase implements FileEditor {

    private final JComponent component;
    private final TestRunEditorUI ui;
    private final VirtualFileImpl virtualFile;

    public FileEditorImpl(VirtualFileImpl vf) {
        this.virtualFile = vf;
        this.ui = new TestRunEditorUI(vf);
        this.component = ui.createEditorPanel();
    }

    @Override
    public @NotNull JComponent getComponent() {
        return component;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return component;
    }

    @Override
    public @NotNull String getName() {
        return "Test Run Editor";
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return virtualFile;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public void dispose() {
        ui.dispose();
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener l) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener l) {
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
    }
}
