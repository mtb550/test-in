package testGit.editorPanel.testPlanEditor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.util.UserDataHolderBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;

public class FileEditorImpl extends UserDataHolderBase implements FileEditor {
    private final JComponent component;

    public FileEditorImpl(VirtualFileImpl virtualFileImpl) {
        TestPlanUI ui = new TestPlanUI();
        this.component = ui.createEditorPanel(virtualFileImpl.getTestCasesTreeModel(), virtualFileImpl.getPlanPath());
    }

    @Override
    public @NotNull JComponent getComponent() {
        return component;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return null;
    }

    @Override
    public @NotNull String getName() {
        return "Test Plan Editor";
    }

    @Override
    public void setState(@NotNull FileEditorState state) {

    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    @Override
    public boolean isModified() {
        return false;
    }
}