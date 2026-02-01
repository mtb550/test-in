package testGit.projectPanel.testPlan;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;

public class TestRunFileEditorProvider implements FileEditorProvider {
    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return file instanceof com.intellij.testFramework.LightVirtualFile && file.getName().startsWith("Test Run:");
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        JComponent component = TestRunEditorService.getInstance(project).getEditorComponent(file);
        return new TextEditorWrapper(component);
    }

    @Override
    public @NotNull String getEditorTypeId() {
        return "test-run-editor";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }

    private static class TextEditorWrapper implements FileEditor {
        private final JComponent component;
        private final java.util.Map<Key<?>, Object> userData = new java.util.HashMap<>();

        public TextEditorWrapper(JComponent component) {
            this.component = component;
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
            return "Test Run Editor";
        }

        @Override
        public void setState(@NotNull FileEditorState state) {
        }

        @Override
        public boolean isModified() {
            return false;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void selectNotify() {
        }

        @Override
        public void deselectNotify() {
        }

        @Override
        public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        }

        @Override
        public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        }

        @Override
        public void dispose() {
        }

        // Proper user data implementation
        @Override
        public <T> T getUserData(@NotNull Key<T> key) {
            return (T) userData.get(key);
        }

        @Override
        public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
            if (value == null) {
                userData.remove(key);
            } else {
                userData.put(key, value);
            }
        }
    }

}
