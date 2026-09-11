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
 * Steps the View Panel on to the next of the cases it was handed.
 * <p>
 * In {@code view} rather than {@code editor/statusbar}, where it and its twin
 * sat until #291. Nothing about them was the editor's status bar: they drive
 * {@link ViewPagination}, they are built only by {@link ViewPanelActions}, and
 * both of their markers already said so - {@code UC-VIEW-PANEL-003} on a class
 * filed under the editor panel is the rule naming the package it belongs in.
 */
public class NextTestCaseAction extends DumbAwareAction {
    private final @NotNull ViewPagination controller;

    public NextTestCaseAction(final @NotNull ViewPagination controller, final @NotNull JComponent component) {
        super(Bundle.message("page.next.case"), Bundle.message("page.next.case.description"), AllIcons.Actions.Forward);
        this.controller = controller;

        this.registerCustomShortcutSet(Shortcuts.Next.getCustomShortcut(), component);
    }

    // UC-VIEW-PANEL-003
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        controller.goNext();
    }

    // UC-VIEW-PANEL-003, Rule-VIEW-PANEL-020
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(controller.hasNext());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // EDT although update() reads no Swing component: ViewPagination's index
        // and item list are plain fields mutated on the EDT, so a background
        // read would enable the button from a stale position (#52).
        return ActionUpdateThread.EDT;
    }
}