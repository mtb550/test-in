package org.testin.actions.clipboard;

import com.intellij.openapi.actionSystem.ActionUpdateThread;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;

public class RedoDetails extends DumbAwareAction {
    TestCaseDto tc;

    public RedoDetails(TestCaseDto tc) {
        super("↪ Redo");
        this.tc = tc;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        ///TODO: TO BE IMPLEMENTED
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
