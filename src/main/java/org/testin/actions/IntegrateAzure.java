package org.testin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class IntegrateAzure extends DumbAwareAction {
    public IntegrateAzure() {
        super("From Azure DevOps");
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        /// TODO: From Azure DevOps
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
