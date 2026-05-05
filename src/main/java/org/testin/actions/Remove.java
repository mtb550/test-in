package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.dirs.*;
import org.testin.util.Tools;
import org.testin.util.TreeUtilImpl;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.Arrays;
import java.util.List;

import static org.testin.util.KeyboardSet.DeletePackage;

public class Remove extends DumbAwareAction {
    private final SimpleTree tree;

    public Remove(SimpleTree tree) {
        super("Remove", "Remove selected nodes", AllIcons.Actions.GC);
        this.tree = tree;
        this.registerCustomShortcutSet(DeletePackage.getCustomShortcut(), tree);
    }

    private boolean isRemovable(Object dir) {
        return dir instanceof DirectoryDto &&
                !(dir instanceof TestProjectDirectoryDto) &&
                !(dir instanceof TestCasesMainDirectoryDto) &&
                !(dir instanceof TestRunsMainDirectoryDto);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null || paths.length == 0) return;

        List<DefaultMutableTreeNode> nodesToRemove = Arrays.stream(paths)
                .map(TreePath::getLastPathComponent)
                .filter(DefaultMutableTreeNode.class::isInstance)
                .map(DefaultMutableTreeNode.class::cast)
                .filter(node -> isRemovable(node.getUserObject()))
                .toList();

        if (nodesToRemove.isEmpty()) return;

        String message = nodesToRemove.size() == 1
                ? "Are you sure you want to remove '" + ((DirectoryDto) nodesToRemove.getFirst().getUserObject()).getName() + "'?"
                : "Are you sure you want to remove these " + nodesToRemove.size() + " items?";

        int confirm = Messages.showYesNoDialog(message, "Confirm Removing", Messages.getQuestionIcon());

        if (confirm == Messages.YES) {

            for (DefaultMutableTreeNode node : nodesToRemove) {
                DirectoryDto pkg = (DirectoryDto) node.getUserObject();

                if (pkg instanceof TestSetDirectoryDto || pkg instanceof TestRunDirectoryDto)
                    Tools.closeEditor(pkg.getName());

                TreeUtilImpl.removeVf(this, pkg.getPath());
                TreeUtilImpl.removeNode(node, tree);
            }
            System.out.println("Removed " + nodesToRemove.size() + " nodes.");
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        TreePath[] paths = tree.getSelectionPaths();

        boolean canRemove = paths != null && Arrays.stream(paths)
                .map(TreePath::getLastPathComponent)
                .filter(DefaultMutableTreeNode.class::isInstance)
                .map(node -> ((DefaultMutableTreeNode) node).getUserObject())
                .anyMatch(this::isRemovable);

        e.getPresentation().setEnabled(canRemove);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}