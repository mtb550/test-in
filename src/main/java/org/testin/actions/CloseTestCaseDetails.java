package org.testin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;
import org.testin.viewPanel.ViewPanel;
import org.testin.viewPanel.ViewToolWindowFactory;

public class CloseTestCaseDetails extends DumbAwareAction {

    public CloseTestCaseDetails(final @NotNull JBList<TestCaseDto> list) {
        super("Close View Panel");
        this.registerCustomShortcutSet(KeyboardSet.Escape.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        ViewPanel viewer = ViewToolWindowFactory.getViewPanel();

        if (viewer != null)
            viewer.hide().reset();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}