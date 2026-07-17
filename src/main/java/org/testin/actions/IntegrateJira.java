package org.testin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class IntegrateJira extends DumbAwareAction {
    public IntegrateJira() {
        super("From Jira");
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        /// TODO: From Jira
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
