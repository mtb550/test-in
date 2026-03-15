package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import testGit.projectPanel.ProjectPanel;

public class CollapseAll extends DumbAwareAction {
    ProjectPanel projectPanel;

    public CollapseAll(ProjectPanel projectPanel) {
        super("Collapse All", "Collapse all nodes", AllIcons.Actions.Collapseall);
        this.projectPanel = projectPanel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (projectPanel.getProjectTree() == null) return;

        SimpleTree tree = projectPanel.getProjectTree().getMainTree();

        if (tree != null) {
            TreeUtil.collapseAll(tree, 0);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        boolean hasTree = projectPanel.getProjectTree() != null && projectPanel.getProjectTree().getMainTree() != null;
        e.getPresentation().setEnabled(hasTree);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}