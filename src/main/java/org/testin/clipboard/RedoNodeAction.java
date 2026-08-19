package org.testin.clipboard;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.explorer.tree.TreeUndoService;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class RedoNodeAction extends AbstractProjectAction {

    private static final KeyStroke SHORTCUT = KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK);

    public RedoNodeAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, "Redo", "Redo last action", AllIcons.Actions.Redo);
        this.registerCustomShortcutSet(Shortcuts.customShortcut(SHORTCUT), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final TreeUndoService redo = Services.getInstance(p, TreeUndoService.class);

        // Asked before, not after: redo() returns silently on an empty stack, so
        // notifying unconditionally would claim a redo that never ran. The
        // presentation is disabled in that case, but a shortcut can still fire.
        if (!redo.canRedo()) return;

        redo.redo();
        Services.getInstance(p, Notifier.class).softShow(p, "Redone");
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final TreeUndoService redo = Services.getInstance(p, TreeUndoService.class);
        e.getPresentation().setEnabled(redo.canRedo());
        e.getPresentation().setText(redo.canRedo() ? "Redo " + redo.redoDescription() : "Redo");
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // The undo stacks mutate on the EDT; reading them there avoids races.
        return ActionUpdateThread.EDT;
    }

}
