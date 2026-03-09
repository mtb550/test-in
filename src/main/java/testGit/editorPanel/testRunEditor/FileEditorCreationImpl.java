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

public class FileEditorCreationImpl extends UserDataHolderBase implements FileEditor {
    private final JComponent component;
    private final TestRunCreationUI ui;
    private final VirtualFileImpl virtualFile;

    public FileEditorCreationImpl(VirtualFileImpl vf) {
        System.out.println("FileEditorCreationImpl.FileEditorCreationImpl()");
        this.virtualFile = vf;
        this.ui = new TestRunCreationUI(vf.getTestCases());
        this.ui.setMetadata(vf.getMetadata());
        this.ui.setCurrentFile(vf);
        this.component = ui.createEditorPanel(vf.getTestCasesTreeModel(), vf.getRunPath(), vf.getProjectPanel());
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