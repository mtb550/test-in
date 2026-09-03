package org.testin.rename;

import org.testin.notifications.Done;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.explorer.ExplorerPanel;
import org.testin.undo.UndoScope;
import org.testin.undo.UndoService;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Optional;

public class RenameAction extends AbstractProjectTreeAction {

    private static final @NotNull KeyStroke SHORTCUT = KeyStroke.getKeyStroke(KeyEvent.VK_F6, InputEvent.SHIFT_DOWN_MASK);
    private final @NotNull ExplorerPanel pp;

    public RenameAction(final @NotNull Project p, final @NotNull ExplorerPanel pp, final @NotNull SimpleTree tree) {
        super(p, tree, "Rename", "Rename selected node", AllIcons.Actions.Edit);
        this.pp = pp;
        this.registerCustomShortcutSet(Shortcuts.customShortcut(SHORTCUT), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        TreeValueUtil.selectedDirectory(tree)
                .filter(DirectoryDto::isRenamable)
                .ifPresent(dir -> new RenameDialog(p, dir.getName(), newName -> renameNode(dir, newName)).show());
    }

    private void renameNode(final @NotNull DirectoryDto dir, final @NotNull String newName) {
        if (newName.isBlank() || newName.equals(dir.getName())) return;

        // No parent means a filesystem root, which is not a node this tree can
        // rename. Asked first because applyRename resolves the new path against
        // the parent and would throw on null - the collision check below already
        // guarded for it while the rename itself did not (#66, F3).
        final @NotNull Optional<Path> found = Optional.ofNullable(dir.getPath().getParent());
        if (found.isEmpty()) {
            Logger.warn("Rename refused, no parent directory: " + dir.getPath());
            return;
        }

        final @NotNull Path parent = found.orElseThrow();

        // A sibling with the new name would make the VFS rename fail with
        // "already exists" - reject it with a message instead. Existence comes
        // from the indexer cache - file access is the indexer's alone.
        if (Services.getInstance(p, ProjectIndexer.class).nodeExists(parent.resolve(newName))) {
            Services.getInstance(p, Notifier.class).softShowExists(p, newName);
            return;
        }

        final @NotNull String oldName = dir.getName();
        NodeRename.apply(p, pp, dir, newName, () -> Services.getInstance(p, Notifier.class).softShow(p, Done.RENAMED));

        // The dto reference stays valid across renames, so undo and redo are
        // the same routine with the names swapped.
        Services.getInstance(p, UndoService.class).push(UndoScope.TREE, new UndoService.Operation(
                "Rename '" + oldName + "'",
                () -> applyRename(dir, oldName),
                () -> applyRename(dir, newName)));
    }

    /**
     * The undo and redo reverses pass no {@code onDone}: they are confirmed as
     * "Undone" and "Redone" by their own actions, and a second balloon saying it
     * was renamed would double-report one keystroke (#62).
     */
    private void applyRename(final @NotNull DirectoryDto dir, final @NotNull String newName) {
        NodeRename.apply(p, pp, dir, newName, () -> {
        });
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TreeValueUtil.selectedDirectory(tree)
                .filter(DirectoryDto::isRenamable)
                .isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }


}
