package org.testin.rename;

import org.testin.notifications.Done;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.project.DumbAwareAction;
import org.testin.actions.TestinData;
import org.testin.explorer.TreePanel;
import org.testin.undo.UndoScope;
import org.testin.undo.UndoService;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.Optional;

/**
 * UC-TREE-PANEL-011.
 * <p>
 * Declared in {@code plugin.xml} (#119), which is what puts it in Find Action
 * and makes its key remappable in Settings -> Keymap. That is why it has no
 * constructor and no fields: the platform builds one instance for the whole
 * IDE, so what it acts on has to come from the keystroke rather than from
 * whoever built it.
 * <p>
 * Its name, description, icon and default key are in the XML for the same
 * reason - the Keymap page reads them from there, and a second copy in a
 * {@code super(...)} call is one of them going stale.
 */
public class RenameAction extends DumbAwareAction {

    // UC-TREE-PANEL-011
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @NotNull Optional<Project> project = Optional.ofNullable(e.getProject());
        if (project.isEmpty()) return;

        final @NotNull Project p = project.orElseThrow();

        TestinData.singleSelectedNode(e)
                .filter(DirectoryDto::isRenamable)
                .ifPresent(dir -> new RenameDialog(p, dir, newName -> renameNode(p, dir, newName)).show());
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-004
    private void renameNode(final @NotNull Project p, final @NotNull DirectoryDto dir, final @NotNull String newName) {
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
            Services.getInstance(p, Notifier.class).softRefuse(p, Refused.ALREADY_EXISTS, newName);
            return;
        }

        final @NotNull String oldName = dir.getName();
        final @NotNull TreePanel tp = Services.getInstance(p, TreePanel.class);

        NodeRename.apply(p, tp, dir, newName, () -> Services.getInstance(p, Notifier.class).softShow(p, Done.RENAMED));

        // The dto reference stays valid across renames, so undo and redo are
        // the same routine with the names swapped.
        Services.getInstance(p, UndoService.class).push(UndoScope.TREE, new UndoService.Operation(
                "Rename '" + oldName + "'",
                () -> applyRename(p, dir, oldName),
                () -> applyRename(p, dir, newName)));
    }

    /**
     * UC-TREE-PANEL-011, Rule-TREE-PANEL-037.
     * <p>
     * The undo and redo reverses pass no {@code onDone}: they are confirmed as
     * "Undone" and "Redone" by their own actions, and a second balloon saying it
     * was renamed would double-report one keystroke (#62).
     */
    private void applyRename(final @NotNull Project p, final @NotNull DirectoryDto dir, final @NotNull String newName) {
        NodeRename.apply(p, Services.getInstance(p, TreePanel.class), dir, newName, () -> {
        });
    }

    /**
     * UC-TREE-PANEL-011, Rule-TREE-PANEL-035.
     * <p>
     * Renaming is about one node, so several selected grays it rather than
     * quietly renaming the first (#192).
     * <p>
     * Now also the guard that keeps a declared key to itself: the answer is
     * empty when the keystroke arrived anywhere but the Testin tree, so this is
     * gray in a Java file rather than renaming whatever the tree happens to hold
     * behind it (#119).
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TestinData.singleSelectedNode(e)
                .filter(DirectoryDto::isRenamable)
                .isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

}
