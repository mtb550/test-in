package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;

public class FileEditorOpeningImpl extends UserDataHolderBase implements FileEditor {
    private final JComponent component;
    private final TestRunOpeningUI ui;
    private final VirtualFileImpl virtualFile;

    public FileEditorOpeningImpl(VirtualFileImpl vf) {
        System.out.println("FileEditorOpeningImpl.FileEditorOpeningImpl()");
        this.virtualFile = vf;
        this.ui = new TestRunOpeningUI(vf);
        this.component = ui.createEditorPanel();
    }

    @Override
    public @NotNull JComponent getComponent() {
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
    public void dispose() {
        Disposer.dispose(ui);
    }

    @Override
    public boolean isModified() {
        return true;
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

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return component;
    }
}