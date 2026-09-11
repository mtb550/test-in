package org.testin.view;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.*;

/**
 * Steps the View Panel back to the previous of the cases it was handed. The
 * twin of {@link NextTestCaseAction}, and moved here with it (#291).
 */
public class PreviousTestCaseAction extends DumbAwareAction {
    private final @NotNull ViewPagination controller;

    public PreviousTestCaseAction(final @NotNull ViewPagination controller, final @NotNull JComponent component) {
        super(Bundle.message("page.previous.case"), Bundle.message("page.previous.case.description"), AllIcons.Actions.Back);
        this.controller = controller;

        this.registerCustomShortcutSet(Shortcuts.Previous.getCustomShortcut(), component);

    }

    // UC-VIEW-PANEL-003
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        controller.goPrevious();
    }

    // UC-VIEW-PANEL-003, Rule-VIEW-PANEL-020
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