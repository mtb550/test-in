package org.testin.rename;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.codegen.clazz.RenameJavaClass;
import org.testin.codegen.pkg.RenameJavaPackage;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.mappers.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.projectPanel.ProjectPanel;
import org.testin.projectPanel.tree.TreeUndoService;
import org.testin.projectPanel.tree.TreeValueUtil;
import org.testin.services.Services;
import org.testin.util.EditorUtil;
import org.testin.util.OptionalPlugin;
import org.testin.util.Tools;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Path;

public class RenameAction extends AbstractProjectAction {

    private static final KeyStroke SHORTCUT = KeyStroke.getKeyStroke(KeyEvent.VK_F6, InputEvent.SHIFT_DOWN_MASK);
    private final @NotNull ProjectPanel pp;
    private final @NotNull SimpleTree tree;

    public RenameAction(final @NotNull Project p, final @NotNull ProjectPanel pp, final @NotNull SimpleTree tree) {
        super(p, "Rename", "Rename selected node", AllIcons.Actions.Edit);
        this.pp = pp;
        this.tree = tree;
        this.registerCustomShortcutSet(Tools.customShortcut(SHORTCUT), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        final TreePath path = tree.getSelectionPath();
        if (path == null) return;

        final DirectoryDto dir = TreeValueUtil.directoryOf(path.getLastPathComponent());
        if (dir == null || !dir.isRenamable()) return;

        new RenameDialog(p, dir.getName(), newName -> renameNode(dir, newName)).show();
    }

    private void renameNode(final @NotNull DirectoryDto dir, final @NotNull String newName) {
        if (newName.isBlank() || newName.equals(dir.getName())) return;

        // A sibling with the new name would make the VFS rename fail with
        // "already exists" - reject it with a message instead. Existence comes
        // from the indexer cache - file access is the indexer's alone.
        final Path parent = dir.getPath().getParent();
        if (parent != null && Services.getInstance(p, ProjectIndexer.class).nodeExists(parent.resolve(newName))) {
            Services.getInstance(p, Notifier.class).softShow(p,
                    "'" + newName + "' already exists in '" + parent.getFileName() + "'");
            return;
        }

        final String oldName = dir.getName();
        applyRename(dir, newName);

        // The dto reference stays valid across renames, so undo and redo are
        // the same routine with the names swapped.
        Services.getInstance(p, TreeUndoService.class).push(new TreeUndoService.TreeOperation(
                "Rename '" + oldName + "'",
                () -> applyRename(dir, oldName),
                () -> applyRename(dir, newName)));
    }

    private void applyRename(final @NotNull DirectoryDto dir, final @NotNull String newName) {
        Services.getInstance(p, EditorUtil.class).close(p, dir.getName());

        dispatchRenameCodeGenerator(dir, newName);

        final Path oldPath = dir.getPath();
        final Path newPath = oldPath.getParent().resolve(newName);

        // The tree refreshes only after the indexer finished the VFS rename
        // and updated its cache - refreshing earlier shows stale state.
        Services.getInstance(p, ProjectIndexer.class).renameNode(oldPath, newPath, () -> {
            pp.getProjectTree().refresh();

            if (dir instanceof TestProjectDirectoryDto) {
                pp.getTestProjectSelector().loadTestProjectList();
            }

            Logger.info("Success! Renamed to: " + newName);
        });
    }

    // todo, to be moved to the codegen package and enhanced, later (#51)
    private void dispatchRenameCodeGenerator(final @NotNull DirectoryDto dir, final @NotNull String newName) {
        if (!OptionalPlugin.JAVA.isAvailableOrWarnOnce(p)) return;

        if (dir instanceof TestProjectDirectoryDto || dir instanceof TestSetPackageDirectoryDto) {
            new RenameJavaPackage().execute(p, dir, newName);
        } else if (dir instanceof TestSetDirectoryDto tsDir) {
            new RenameJavaClass().execute(p, tsDir, newName);
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        e.getPresentation().setEnabled(path != null &&
                TreeValueUtil.directoryOf(path.getLastPathComponent()) instanceof DirectoryDto dir &&
                dir.isRenamable()
        );
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }


}
