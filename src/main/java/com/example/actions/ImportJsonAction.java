package com.example.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ImportJsonAction extends AnAction {
    public ImportJsonAction() {
        super("From Json");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: Import test cases From Json
    }
}
