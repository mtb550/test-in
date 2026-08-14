package org.testin.remove;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.projectPanel.tree.TreeValueUtil;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.EditorUtil;

import javax.swing.tree.TreePath;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testin.util.Shortcuts.DeletePackage;

public class RemoveAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull SimpleTree tree;
    private final @NotNull ProjectPanel pp;

    public RemoveAction(final @NotNull Project p, final @NotNull SimpleTree tree, final @NotNull ProjectPanel pp) {
        super("Remove", "Remove selected nodes", AllIcons.Actions.GC);
        this.p = p;
        this.tree = tree;
        this.pp = pp;
        this.registerCustomShortcutSet(DeletePackage.getCustomShortcut(), tree);
    }

    private boolean isRemovable(final @Nullable Object dir) {
        return dir instanceof DirectoryDto dto && dto.isRemovable();
    }

    private @NotNull List<DirectoryDto> getRemovableNodes(final TreePath @Nullable [] paths) {
        return TreeValueUtil.selectedDirectories(paths).stream()
                .filter(this::isRemovable)
                .toList();
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null || paths.length == 0) return;

        List<DirectoryDto> nodesToRemove = getRemovableNodes(paths);
        if (nodesToRemove.isEmpty()) return;

        final String msg = nodesToRemove.size() == 1
                ? "Remove '" + nodesToRemove.getFirst().getName() + "'?"
                : "Remove these " + nodesToRemove.size() + " items?";

        // Single node: its path shows exactly what is being deleted.
        final String from = nodesToRemove.size() == 1 ? nodesToRemove.getFirst().getPath().toString() : null;

        new ConfirmDialog(p, "Confirm Removing", msg, from, null, "Remove", () -> removeNodes(nodesToRemove)).show();
    }

    private void removeNodes(final @NotNull List<DirectoryDto> nodesToRemove) {
        if (nodesToRemove.isEmpty()) return;

        // Removal is asynchronous now, so the tree is rebuilt once the last node
        // is actually gone rather than immediately after the loop - at which
        // point none of them would have been removed yet.
        final AtomicInteger pending = new AtomicInteger(nodesToRemove.size());
        final Runnable onRemoved = () -> {
            if (pending.decrementAndGet() != 0) return;

            pp.getProjectTree().updateNodes();
            Logger.info("Removed " + nodesToRemove.size() + " node(s).");
        };

        for (final DirectoryDto pkg : nodesToRemove) {

            if (pkg instanceof TestSetDirectoryDto || pkg instanceof TestRunDirectoryDto)
                Services.getInstance(p, EditorUtil.class).close(p, pkg.getName());

            pkg.getType().getRemoveHandler().remove(p, pkg, onRemoved);
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        TreePath[] paths = tree.getSelectionPaths();
        boolean enabled = paths != null && !getRemovableNodes(paths).isEmpty();
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
