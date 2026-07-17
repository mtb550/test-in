package org.testin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class IntegrateTestRail extends DumbAwareAction {
    public IntegrateTestRail() {
        super("From Test Rail");
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        /// TODO: From Test Rail
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
