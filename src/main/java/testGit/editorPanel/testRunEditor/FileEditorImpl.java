package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.util.UserDataHolderBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;

public class FileEditorImpl extends UserDataHolderBase implements FileEditor {
    private final JComponent component;
    private final TestRunUI ui;

    public FileEditorImpl(VirtualFileImpl virtualFileImpl) {
        this.ui = new TestRunUI();
        this.component = ui.createEditorPanel(virtualFileImpl.getTestCasesTreeModel(), virtualFileImpl.getRunPath());
    }

    @Override
    public @NotNull JComponent getComponent() {
        return component;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return ui.getChecklistTree();
    }

    @Override
    public @NotNull String getName() {
        return "Test Run Editor";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener l) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener l) {
    }
}