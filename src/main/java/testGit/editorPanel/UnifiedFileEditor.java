package testGit.editorPanel;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.dto.TestCaseDto;
import testGit.viewPanel.ViewToolWindowFactory;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Optional;

public class UnifiedFileEditor extends UserDataHolderBase implements FileEditor {

    private final IEditorUI ui;
    private final UnifiedVirtualFile vf;

    public UnifiedFileEditor(final UnifiedVirtualFile vf, final IEditorUI ui) {
        this.vf = vf;
        this.ui = ui;
    }

    @Override
    public @NotNull JComponent getComponent() {
        return ui.getComponent();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return ui.getPreferredFocusedComponent();
    }

    @Override
    public @NotNull String getName() {
        return vf.getDirectoryDto().getName();
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
        ui.dispose();
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
        final List<TestCaseDto> selected = ui.getSelectedTestCases();

        Optional.ofNullable(ViewToolWindowFactory.getToolWindow())
                .map(tw -> ViewToolWindowFactory.getViewPanel())
                .ifPresent(viewer -> {

                    if (selected != null && !selected.isEmpty())
                        viewer.show(selected, vf.getDirectoryDto().getPath());

                    else
                        viewer.reset();

                });
    }
}