package org.testin.crud;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.dirs.*;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.EditorUtil;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.Arrays;
import java.util.List;

import static org.testin.util.KeyboardSet.DeletePackage;

public class RemoveAction extends DumbAwareAction {
    private final @NotNull SimpleTree tree;
    private final @NotNull ProjectPanel projectPanel;

    public RemoveAction(final @NotNull SimpleTree tree, final @NotNull ProjectPanel projectPanel) {
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
    public void actionPerformed(final @NotNull AnActionEvent e) {
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
                Services.getInstance(project, EditorUtil.class).close(project, pkg.getName());

            final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);

            // The indexer owns the file/dir removal + in-memory store.
            switch (pkg) {
                case TestProjectDirectoryDto ignored -> indexer.removeTestProject(pkg.getPath());
                case TestSetDirectoryDto ignored -> indexer.removeTestSet(pkg.getPath());
                case TestRunDirectoryDto ignored -> indexer.removeTestRun(pkg.getPath());
                case TestSetPackageDirectoryDto ignored -> indexer.removeTestSetPackage(pkg.getPath());
                case TestRunPackageDirectoryDto ignored -> indexer.removeTestRunPackage(pkg.getPath());
                default -> {
                }
            }

            if (node.getParent() != null)
                indexer.removeNode(node, tree);
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            projectPanel.getProjectTree().updateNodes();
            Logger.info("Removed " + nodesToRemove.size() + " node(s).");
        });

        // todo, remove java method and packages once delete node, code generator to be added
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