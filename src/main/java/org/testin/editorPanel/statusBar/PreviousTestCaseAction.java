package org.testin.editorPanel.statusBar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;
import org.testin.viewPanel.ViewPagination;

import javax.swing.*;

public class PreviousTestCaseAction extends DumbAwareAction {
    private final ViewPagination controller;

    public PreviousTestCaseAction(final ViewPagination controller, final JComponent component) {
        super("Previous Test Case", "Previous test case", AllIcons.Actions.Back);
        this.controller = controller;

        if (component != null)
            this.registerCustomShortcutSet(Shortcuts.PreviousTestCase.getCustomShortcut(), component);

    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (controller != null) controller.goPrevious();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(controller != null && controller.hasPrevious());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}