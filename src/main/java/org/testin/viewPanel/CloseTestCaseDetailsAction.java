package org.testin.viewPanel;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.KeyboardSet;

public class CloseTestCaseDetailsAction extends DumbAwareAction {

    public CloseTestCaseDetailsAction(final @NotNull JBList<TestCaseDto> list) {
        super("Close View Panel");
        this.registerCustomShortcutSet(KeyboardSet.Escape.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final ViewPanel viewer = ViewToolWindowFactory.getViewPanel();

        if (viewer != null)
            viewer.hide().reset();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}