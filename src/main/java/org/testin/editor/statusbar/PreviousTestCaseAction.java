package org.testin.editor.statusbar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;
import org.testin.view.ViewPagination;

import javax.swing.*;

public class PreviousTestCaseAction extends DumbAwareAction {
    private final @NotNull ViewPagination controller;

    public PreviousTestCaseAction(final @NotNull ViewPagination controller, final @NotNull JComponent component) {
        super("Previous Test Case", "Previous test case", AllIcons.Actions.Back);
        this.controller = controller;

        this.registerCustomShortcutSet(Shortcuts.PreviousTestCase.getCustomShortcut(), component);

    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        controller.goPrevious();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(controller.hasPrevious());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // EDT although update() reads no Swing component: ViewPagination's index
        // and item list are plain fields mutated on the EDT, so a background
        // read would enable the button from a stale position (#52).
        return ActionUpdateThread.EDT;
    }
}