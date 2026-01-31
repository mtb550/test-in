package com.example.actions.editorPanel;

import com.example.pojo.TestCase;
import com.example.util.ActionHistory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class UndoDetailsAction extends AnAction {
    TestCase tc;

    public UndoDetailsAction(TestCase tc) {
        super("↩ Undo");
        this.tc = tc;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ActionHistory.undo();
    }
}
