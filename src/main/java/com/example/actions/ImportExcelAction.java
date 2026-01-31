package com.example.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ImportExcelAction extends AnAction {
    public ImportExcelAction() {
        super("From Excel");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: Import test cases From Excel
    }
}
