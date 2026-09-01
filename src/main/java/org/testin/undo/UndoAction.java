package org.testin.undo;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Shortcuts;

import javax.swing.*;

/**
 * Takes the last thing back, or puts it forward again, on the surface the
 * tester is standing on.
 * <p>
 * Two things it is told and neither of which it works out: which way it goes,
 * and whose history it reads. That is what lets the project tree and every test
 * editor offer the same pair of keys over histories of their own (#165).
 */
public class UndoAction extends AbstractProjectAction {

    private final @NotNull UndoDirection direction;
    private final @NotNull UndoScope scope;

    public UndoAction(final @NotNull Project p, final @NotNull JComponent on, final @NotNull UndoScope scope, final @NotNull UndoDirection direction) {
        super(p, direction.getTitle(), direction.getTitle() + " last action", direction.getIcon());
        this.direction = direction;
        this.scope = scope;
        this.registerCustomShortcutSet(Shortcuts.customShortcut(direction.getShortcut()), on);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @NotNull UndoService service = Services.getInstance(p, UndoService.class);

        // Asked before, not after: the service returns silently on an empty
        // stack, so notifying unconditionally would claim something that never
        // ran. The presentation is disabled in that case, but a shortcut can
        // still fire.
        if (!direction.can(service, scope)) return;

        direction.apply(service, scope);
        Services.getInstance(p, Notifier.class).softShow(p, direction.getDone());
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @NotNull UndoService service = Services.getInstance(p, UndoService.class);

        e.getPresentation().setEnabled(direction.can(service, scope));
        e.getPresentation().setText((direction.getTitle() + " " + direction.next(service, scope)).trim());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // The undo stacks mutate on the EDT; reading them there avoids races.
        return ActionUpdateThread.EDT;
    }
}
