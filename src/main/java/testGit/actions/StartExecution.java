package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.testRunEditor.RunEditorUI;
import testGit.editorPanel.toolBar.IToolBar;

public class StartExecution extends DumbAwareAction {
    private final IToolBar callbacks;

    public StartExecution(final IToolBar callbacks) {
        super("Start Run", "Start execution of test cases", AllIcons.Nodes.Services);
        this.callbacks = callbacks;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        callbacks.onStartExecutionClicked();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(callbacks instanceof RunEditorUI);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}