package org.testin.run;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.editor.toolbar.Toolbar;

public class StartExecutionAction extends DumbAwareAction {
    private final @NotNull Toolbar callbacks;

    public StartExecutionAction(final @NotNull Toolbar callbacks) {
        super(Toolbar.START_MANUAL_EXECUTION,
                "Walk this run a case at a time, timing each one. Runs no automation - Run Test Case does that",
                Toolbar.START_MANUAL_EXECUTION_ICON);
        this.callbacks = callbacks;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        callbacks.onStartExecutionClicked();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(callbacks instanceof RunEditor editor && editor.canStartExecution());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // EDT although update() reads no Swing component: canStartExecution reads
        // the execution index, which the EDT writes as each test case starts, so a
        // background read would offer Start from a stale position (#52).
        return ActionUpdateThread.EDT;
    }
}