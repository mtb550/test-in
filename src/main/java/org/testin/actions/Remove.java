package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.dirs.*;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.EditorUtil;
import org.testin.util.TreeUtilImpl;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.Arrays;
import java.util.List;

import static org.testin.util.KeyboardSet.DeletePackage;

public class Remove extends DumbAwareAction {
    private final SimpleTree tree;
    private final ProjectPanel projectPanel;

    public Remove(final SimpleTree tree, final ProjectPanel projectPanel) {
        super("Remove", "Remove selected nodes", AllIcons.Actions.GC);
        this.tree = tree;
        this.projectPanel = projectPanel;
        this.registerCustomShortcutSet(DeletePackage.getCustomShortcut(), tree);
    }

    private boolean isRemovable(final Object dir) {
        return dir instanceof DirectoryDto &&
                !(dir instanceof TestCasesMainDirectoryDto) &&
                !(dir instanceof TestRunsMainDirectoryDto);
    }

    private List<DefaultMutableTreeNode> getRemovableNodes(final TreePath[] paths) {
        return Arrays.stream(paths)
                .map(TreePath::getLastPathComponent)
                .filter(DefaultMutableTreeNode.class::isInstance)
                .map(DefaultMutableTreeNode.class::cast)
                .filter(node -> isRemovable(node.getUserObject()))
                .toList();
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null || paths.length == 0) return;

        List<DefaultMutableTreeNode> nodesToRemove = getRemovableNodes(paths);
        if (nodesToRemove.isEmpty()) return;

        String msg = nodesToRemove.size() == 1
                ? "Remove '" + ((DirectoryDto) nodesToRemove.getFirst().getUserObject()).getName() + "'?"
                : "Remove these " + nodesToRemove.size() + " items?";

        if (Messages.showYesNoDialog(msg, "Confirm Removing", Messages.getQuestionIcon()) != Messages.YES)
            return;

        for (DefaultMutableTreeNode node : nodesToRemove) {
            DirectoryDto pkg = (DirectoryDto) node.getUserObject();

            if (pkg instanceof TestSetDirectoryDto || pkg instanceof TestRunDirectoryDto)
                Services.getInstance(project, EditorUtil.class).closeEditor(project, pkg.getName());

            TreeUtilImpl util = Services.getInstance(project, TreeUtilImpl.class);
            util.removeVf(project, this, pkg.getPath());

            if (node.getParent() != null)
                util.removeNode(node, tree);
            else {
                util.removeRootNode(tree);
                new Refresh(projectPanel).execute();
            }
        }

        VirtualFileManager.getInstance().syncRefresh();
        Log.info("Removed " + nodesToRemove.size() + " node(s).");
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        TreePath[] paths = tree.getSelectionPaths();
        boolean enabled = paths != null && !getRemovableNodes(paths).isEmpty();
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}