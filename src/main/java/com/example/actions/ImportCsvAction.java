package com.example.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ImportCsvAction extends AnAction {
    public ImportCsvAction() {
        super("From CSV");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: Import test cases From CSV
    }
}
