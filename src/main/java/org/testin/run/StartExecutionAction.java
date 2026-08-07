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
        super("Start Run", "Start execution of test cases", AllIcons.Nodes.Services);
        this.p = p;
        this.callbacks = callbacks;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        callbacks.onStartExecutionClicked();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(callbacks instanceof RunEditor);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}