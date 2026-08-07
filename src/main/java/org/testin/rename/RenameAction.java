package org.testin.rename;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.mappers.dto.dirs.TestRunsMainDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.services.Services;
import org.testin.util.EditorUtil;
import org.testin.util.KeyboardSet;
import org.testin.util.Tools;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.nio.file.Path;

public class RenameAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull ProjectPanel pp;
    private final @NotNull SimpleTree tree;

    public RenameAction(final @NotNull Project p, final @NotNull ProjectPanel pp, final @NotNull SimpleTree tree) {
        super("Rename", "Rename selected node", AllIcons.Actions.Edit);
        this.p = p;
        this.pp = pp;
        this.tree = tree;
        this.registerCustomShortcutSet(KeyboardSet.RenameNode.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        final TreePath path = tree.getSelectionPath();
        if (path == null) return;

        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

        if (!(node.getUserObject() instanceof DirectoryDto dir)) return;
        if (dir instanceof TestCasesMainDirectoryDto || dir instanceof TestRunsMainDirectoryDto) return;

        String newName = Messages.showInputDialog("Enter new name:", "Rename", AllIcons.Actions.Edit, dir.getName(), null);
        if (newName == null || newName.isBlank() || newName.equals(dir.getName())) return;

        Services.getInstance(p, EditorUtil.class).close(p, dir.getName());

        Path oldPath = dir.getPath();
        Path newPath = oldPath.getParent().resolve(newName);

        Services.getInstance(p, ProjectIndexer.class).renameNode(oldPath, newPath);

        Services.getInstance(p, Tools.class).updateChildrenPathsRecursive(node, oldPath, newPath);
        ((DefaultTreeModel) tree.getModel()).nodeChanged(node);

        if (dir instanceof TestProjectDirectoryDto) {
            pp.getTestProjectSelector().loadTestProjectList();
        }

        Logger.info("Success! Renamed to: " + newName);

        // todo: update indexer and its child after rename
        // todo: add code generator code to change the name in automation code.
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        e.getPresentation().setEnabled(path != null &&
                path.getLastPathComponent() instanceof DefaultMutableTreeNode node &&
                node.getUserObject() instanceof DirectoryDto dir &&
                !(dir instanceof TestCasesMainDirectoryDto) &&
                !(dir instanceof TestRunsMainDirectoryDto)
        );
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }


}