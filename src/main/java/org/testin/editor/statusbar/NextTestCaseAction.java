package org.testin.editor.statusbar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.util.Shortcuts;
import org.testin.view.ViewPagination;

import javax.swing.*;

public class NextTestCaseAction extends DumbAwareAction {
    private final @Nullable ViewPagination controller;

    public NextTestCaseAction(final @Nullable ViewPagination controller, final @Nullable JComponent component) {
        super("Next Test Case", "Go to next test case", AllIcons.Actions.Forward);
        this.controller = controller;

        if (component != null) {
            this.registerCustomShortcutSet(Shortcuts.NextTestCase.getCustomShortcut(), component);
        }
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (controller != null) controller.goNext();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(controller != null && controller.hasNext());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // EDT although update() reads no Swing component: ViewPagination's index
        // and item list are plain fields mutated on the EDT, so a background
        // read would enable the button from a stale position (#52).
        return ActionUpdateThread.EDT;
    }
}