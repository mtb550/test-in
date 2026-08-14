package org.testin.run;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.runEditor.RunEditor;
import org.testin.editorPanel.toolBar.IToolBar;

public class StartExecutionAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull IToolBar callbacks;

    public StartExecutionAction(final @NotNull Project p, final @NotNull IToolBar callbacks) {
        super("Start Run", "Start execution of test cases", AllIcons.Actions.Execute);
        this.p = p;
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