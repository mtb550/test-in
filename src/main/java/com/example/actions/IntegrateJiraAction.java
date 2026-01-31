package com.example.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class IntegrateJiraAction extends AnAction {
    public IntegrateJiraAction() {
        super("From Jira");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: From Jira
    }
}
