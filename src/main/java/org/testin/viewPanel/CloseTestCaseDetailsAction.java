package org.testin.viewPanel;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.Shortcuts;

public class CloseTestCaseDetailsAction extends DumbAwareAction {
    private final @NotNull Project p;

    public CloseTestCaseDetailsAction(final @NotNull Project p, final @NotNull JBList<TestCaseDto> list) {
        super("Close View Panel");
        this.p = p;
        this.registerCustomShortcutSet(Shortcuts.Escape.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final ViewPanel viewer = ViewToolWindowFactory.getViewPanel();

        if (viewer != null)
            viewer.hide().reset();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - no update() here reads Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }
}