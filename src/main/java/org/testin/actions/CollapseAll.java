package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.projectPanel.ProjectPanel;

public class CollapseAll extends DumbAwareAction {
    final @NotNull ProjectPanel projectPanel;

    public CollapseAll(final @NotNull ProjectPanel projectPanel) {
        super("Collapse All", "Collapse all nodes", AllIcons.Actions.Collapseall);
        this.projectPanel = projectPanel;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        SimpleTree tree = projectPanel.getProjectTree().getMainTree();

        if (tree != null)
            TreeUtil.collapseAll(tree, 0);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        if (e.getProject() != null) {
            e.getPresentation().setEnabled(projectPanel.getProjectTree().getMainTree() != null);
            return;
        }

        e.getPresentation().setEnabled(false);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}