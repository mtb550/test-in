package org.testin.editorPanel.statusBar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.util.Shortcuts;
import org.testin.viewPanel.ViewPagination;

import javax.swing.*;

public class PreviousTestCaseAction extends DumbAwareAction {
    private final @Nullable ViewPagination controller;

    public PreviousTestCaseAction(final @Nullable ViewPagination controller, final @Nullable JComponent component) {
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
        // EDT although update() reads no Swing component: ViewPagination's index
        // and item list are plain fields mutated on the EDT, so a background
        // read would enable the button from a stale position (#52).
        return ActionUpdateThread.EDT;
    }
}