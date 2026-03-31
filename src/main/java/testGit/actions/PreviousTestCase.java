package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import testGit.util.KeyboardSet;
import testGit.viewPanel.ViewPagination;

import javax.swing.*;

public class PreviousTestCase extends DumbAwareAction {
    private final ViewPagination controller;

    public PreviousTestCase(final ViewPagination controller, final JComponent component) {
        super("Previous Test Case", "Previous test case", AllIcons.Actions.Back);
        this.controller = controller;

        if (component != null)
            this.registerCustomShortcutSet(KeyboardSet.PreviousTestCase.getShortcut(), component);

    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        if (controller != null) controller.goPrevious();
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        e.getPresentation().setEnabled(controller != null && controller.hasPrevious());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}