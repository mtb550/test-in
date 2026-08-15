package org.testin.editor.toolbar.components;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareToggleAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Set;

/**
 * One check-mark entry in the toolbar filter popup: toggles a value in its
 * selection set and notifies the owning button.
 */
final class ToggleFilterAction<T> extends DumbAwareToggleAction {

    private final @NotNull T value;
    private final @NotNull Set<T> selection;
    private final @NotNull FilterMembership<T> membership;
    private final @NotNull Runnable onChanged;

    ToggleFilterAction(final @NotNull String text, final @Nullable Icon icon,
                       final @NotNull T value, final @NotNull Set<T> selection,
                       final @NotNull FilterMembership<T> membership, final @NotNull Runnable onChanged) {
        super(text, null, icon);
        this.value = value;
        this.selection = selection;
        this.membership = membership;
        this.onChanged = onChanged;
    }

    @Override
    public boolean isSelected(final @NotNull AnActionEvent e) {
        return selection.contains(value);
    }

    @Override
    public void setSelected(final @NotNull AnActionEvent e, final boolean state) {
        membership.apply(value, selection, state);
        onChanged.run();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - update() reads only fields/services, never Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }
}
