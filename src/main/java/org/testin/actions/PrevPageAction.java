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

public class PrevPageAction extends DumbAwareAction {
    private final @NotNull IEditor editor;

    public PrevPageAction(final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super("Previous Page", "Navigate to the previous page", AllIcons.Actions.Back);
        this.editor = editor;
        registerCustomShortcutSet(KeyboardSet.PreviousTestCase.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (editor.getCurrentPage() > 1) {
            editor.setCurrentPage(editor.getCurrentPage() - 1);
            editor.refreshView();
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        int current = editor.getCurrentPage();
        e.getPresentation().setEnabled(current > 1);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}