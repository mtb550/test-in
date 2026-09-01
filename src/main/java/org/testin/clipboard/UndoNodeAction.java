package org.testin.clipboard;

import org.testin.notifications.Done;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.undo.UndoService;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class UndoNodeAction extends AbstractProjectAction {

    private static final @NotNull KeyStroke SHORTCUT = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK);

    /**
     * Registered on whatever component the tester is standing in - the project
     * tree, or a test editor's card list. One stack behind both, so the key
     * means the same thing in either (#165), and the component decides only
     * where it is listened for.
     */
    public UndoNodeAction(final @NotNull Project p, final @NotNull JComponent on) {
        super(p, "Undo", "Undo last action", AllIcons.Actions.Undo);
        this.registerCustomShortcutSet(Shortcuts.customShortcut(SHORTCUT), on);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @NotNull UndoService undo = Services.getInstance(p, UndoService.class);

        // Asked before, not after: undo() returns silently on an empty stack, so
        // notifying unconditionally would claim an undo that never ran. The
        // presentation is disabled in that case, but a shortcut can still fire.
        if (!undo.canUndo()) return;

        undo.undo();
        Services.getInstance(p, Notifier.class).softShow(p, Done.UNDONE);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @NotNull UndoService undo = Services.getInstance(p, UndoService.class);
        e.getPresentation().setEnabled(undo.canUndo());
        e.getPresentation().setText(("Undo " + undo.undoDescription()).trim());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // The undo stacks mutate on the EDT; reading them there avoids races.
        return ActionUpdateThread.EDT;
    }

}
