package org.testin.editor.statusbar;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Shortcuts;

import javax.swing.*;

/**
 * Page navigation action for the editors; the subclasses only choose direction.
 */
abstract class AbstractPageAction extends DumbAwareAction {
    protected final @NotNull TestinEditor editor;
    private final int delta;

    protected AbstractPageAction(final @NotNull TestinEditor editor, final @NotNull JBList<TestCaseDto> list, final @NotNull String title, final @NotNull String description, final @NotNull Icon icon, final @NotNull Shortcuts shortcut, final int delta) {
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
        // EDT although update() reads no Swing component: the page state it does
        // read is owned by the EDT - actionPerformed writes currentPage there -
        // so a background read would enable or disable the button from a stale
        // page number (#52).
        return ActionUpdateThread.EDT;
    }
}
