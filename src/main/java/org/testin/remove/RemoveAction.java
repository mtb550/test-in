package org.testin.remove;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.DirectoryType;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.*;
import org.testin.projectPanel.ProjectPanel;
import org.testin.services.Services;
import org.testin.util.EditorUtil;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.Arrays;
import java.util.List;

import static org.testin.util.KeyboardSet.DeletePackage;

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
    public void actionPerformed(final @NotNull AnActionEvent e) {

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
                Services.getInstance(p, EditorUtil.class).close(p, pkg.getName());

            final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

            final DirectoryType type = DirectoryType.from(pkg);
            if (type != null && type.getRemoveHandler() != null)
                type.getRemoveHandler().remove(p, pkg);

            if (node.getParent() != null)
                indexer.removeNode(node, tree);
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            pp.getProjectTree().updateNodes();
            Logger.info("Removed " + nodesToRemove.size() + " node(s).");
        });

    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        TreePath[] paths = tree.getSelectionPaths();
        boolean enabled = paths != null && !getRemovableNodes(paths).isEmpty();
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}