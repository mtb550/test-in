package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditor;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;

public class NextPageAction extends DumbAwareAction {
    private final @NotNull IEditor editor;

    public NextPageAction(final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super("Next Page", "Navigate to the next page", AllIcons.Actions.Forward);
        this.editor = editor;
        registerCustomShortcutSet(KeyboardSet.NextTestCase.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (editor.getCurrentPage() < editor.getTotalPageCount()) {
            editor.setCurrentPage(editor.getCurrentPage() + 1);
            editor.refreshView();
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        int current = editor.getCurrentPage();
        int total = editor.getTotalPageCount();
        e.getPresentation().setEnabled(current < total);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
