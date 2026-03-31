package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import testGit.util.KeyboardSet;
import testGit.viewPanel.ViewPagination;

import javax.swing.*;

public class NextTestCase extends DumbAwareAction {
    private final ViewPagination controller;

    public NextTestCase(final ViewPagination controller, final JComponent component) {
        super("Next Test Case", "Go to next test case", AllIcons.Actions.Forward);
        this.controller = controller;

        if (component != null) {
            this.registerCustomShortcutSet(KeyboardSet.NextTestCase.getShortcut(), component);
        }
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        if (controller != null) controller.goNext();
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        e.getPresentation().setEnabled(controller != null && controller.hasNext());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}