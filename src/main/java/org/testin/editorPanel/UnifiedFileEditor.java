package org.testin.editorPanel;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.viewPanel.ViewToolWindowFactory;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Optional;

public class UnifiedFileEditor extends UserDataHolderBase implements FileEditor {

    private final @NotNull IEditor editor;
    private final @NotNull UnifiedVirtualFile vf;
    private final @NotNull Project project;

    public UnifiedFileEditor(final @NotNull Project p, final @NotNull UnifiedVirtualFile vf, final @NotNull IEditor editor) {
        this.project = p;
        this.vf = vf;
        this.editor = editor;
    }

    @Override
    public @NotNull JComponent getComponent() {
        return editor.getComponent();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return editor.getPreferredFocusedComponent();
    }

    @Override
    public @NotNull String getName() {
        return vf.getDir().getName();
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return vf;
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
        editor.dispose();
    }

    @Override
    public void addPropertyChangeListener(final @NotNull PropertyChangeListener l) {
    }

    @Override
    public void removePropertyChangeListener(final @NotNull PropertyChangeListener l) {
    }

    @Override
    public void setState(final @NotNull FileEditorState state) {
    }

    @Override
    public void selectNotify() {
        final List<TestCaseDto> selected = editor.getSelectedTestCases();

        Optional.ofNullable(ViewToolWindowFactory.getToolWindow(project))
                .map(tw -> ViewToolWindowFactory.getViewPanel())
                .ifPresent(viewer -> {

                    if (selected != null && !selected.isEmpty())
                        viewer.show(selected, vf.getDir().getPath2());

                    else viewer.reset();
                });
    }
}