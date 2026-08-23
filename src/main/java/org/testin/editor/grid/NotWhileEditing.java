package org.testin.editor.grid;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

/**
 * The same action, refused while a grid cell is open for editing (#74).
 * <p>
 * The verdict keys are bare letters. Typing "Passed. The balance is shown." into
 * an Actual Result cell would otherwise set three statuses and turn a page,
 * because the IDE dispatches a registered shortcut before the focused
 * component's own key bindings - and the cell editor lives inside the table the
 * shortcuts are registered on.
 * <p>
 * One wrapper rather than a guard in each action's {@code update}: there are
 * nine of them, an action added to the menu tomorrow would need the tenth, and
 * none of them has any other reason to know a grid exists.
 */
public final class NotWhileEditing extends AnAction {

    private final @NotNull AnAction delegate;
    private final @NotNull JBTable table;

    private NotWhileEditing(final @NotNull AnAction delegate, final @NotNull JBTable table) {
        this.delegate = delegate;
        this.table = table;
    }

    /**
     * Puts this action's own shortcut on the table, guarded.
     * <p>
     * An action with no shortcut registers an empty set, which binds nothing -
     * so a menu can be walked whole without asking which of its entries has a
     * key, and its separators cost nothing.
     */
    public static void bind(final @NotNull JBTable table, final @NotNull AnAction action) {
        new NotWhileEditing(action, table).registerCustomShortcutSet(action.getShortcutSet(), table);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        delegate.update(e);

        // After the delegate, never instead of it: an action that would refuse
        // itself anyway must keep refusing for its own reason.
        if (table.isEditing()) e.getPresentation().setEnabled(false);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        delegate.actionPerformed(e);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // The delegate's, because it is the delegate's update() that runs.
        return delegate.getActionUpdateThread();
    }
}
