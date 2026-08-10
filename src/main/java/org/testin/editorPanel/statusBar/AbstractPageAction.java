package org.testin.editorPanel.statusBar;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditor;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.KeyboardSet;

import javax.swing.*;

/**
 * Page navigation action for the editors; the subclasses only choose direction.
 */
abstract class AbstractPageAction extends DumbAwareAction {
    protected final @NotNull IEditor editor;
    private final int delta;

    protected AbstractPageAction(final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list,
                                 final @NotNull String title, final @NotNull String description, final Icon icon,
                                 final @NotNull KeyboardSet shortcut, final int delta) {
        super(title, description, icon);
        this.editor = editor;
        this.delta = delta;
        registerCustomShortcutSet(shortcut.getCustomShortcut(), list);
    }

    private boolean canNavigate() {
        final int target = editor.getCurrentPage() + delta;
        return target >= 1 && target <= editor.getTotalPageCount();
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (canNavigate()) {
            editor.setCurrentPage(editor.getCurrentPage() + delta);
            editor.refreshView();
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(canNavigate());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
