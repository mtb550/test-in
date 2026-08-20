package org.testin.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.dto.TestCaseDto;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class UnifiedFileEditor extends UserDataHolderBase implements FileEditor {

    private final @NotNull Project p;
    private final @NotNull UnifiedVirtualFile vf;

    /**
     * The Testin editor inside this tab. Exposed so a re-index can tell it to
     * read the node again - the tab is what the platform hands back, and the
     * editor is what holds the data.
     */
    @Getter
    private final @NotNull TestinEditor editor;

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

        Optional.ofNullable(ViewToolWindowFactory.getToolWindow(p))
                .map(tw -> ViewToolWindowFactory.getViewPanel())
                .ifPresent(viewer -> {

                    if (!selected.isEmpty())
                        viewer.show(selected, vf.getDir().getPath2());

                    else viewer.reset();
                });
    }
}
