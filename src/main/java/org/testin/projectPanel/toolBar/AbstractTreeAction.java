package org.testin.projectPanel.toolBar;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.projectPanel.ProjectPanel;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * Toolbar action that operates on the project tree when it is available.
 */
abstract class AbstractTreeAction extends DumbAwareAction {
    private final @NotNull ProjectPanel pp;
    private final @NotNull Consumer<SimpleTree> operation;

    protected AbstractTreeAction(final @NotNull ProjectPanel pp,
                                 final @NotNull String title, final @NotNull String description,
                                 final @NotNull Icon icon,
                                 final @NotNull Consumer<SimpleTree> operation) {
        super(title, description, icon);
        this.pp = pp;
        this.operation = operation;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        operation.accept(pp.getProjectTree().getMainTree());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - this action has no update() reading Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }
}
