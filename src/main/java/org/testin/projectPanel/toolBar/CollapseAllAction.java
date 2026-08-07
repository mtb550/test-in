package org.testin.projectPanel.toolBar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.projectPanel.ProjectPanel;

public class CollapseAllAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull ProjectPanel projectPanel;

    public CollapseAllAction(final @NotNull Project p, final @NotNull ProjectPanel projectPanel) {
        super("Collapse All", "Collapse all nodes", AllIcons.Actions.Collapseall);
        this.p = p;
        this.projectPanel = projectPanel;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final SimpleTree tree = projectPanel.getProjectTree().getMainTree();

        if (tree != null)
            TreeUtil.collapseAll(tree, 0);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(projectPanel.getProjectTree().getMainTree() != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}