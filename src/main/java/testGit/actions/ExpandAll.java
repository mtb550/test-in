package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.projectPanel.ProjectPanel;

public class ExpandAll extends DumbAwareAction {
    ProjectPanel projectPanel;

    public ExpandAll(ProjectPanel projectPanel) {
        super("Expand All", "Expand all nodes", AllIcons.Actions.Expandall);
        this.projectPanel = projectPanel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (projectPanel.getProjectTree() == null) return;

        SimpleTree tree = projectPanel.getProjectTree().getMainTree();

        if (tree != null) {
            for (int i = 0; i < tree.getRowCount(); i++) {
                tree.expandRow(i);
            }
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        if (e.getProject() == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        boolean hasTree = projectPanel.getProjectTree() != null && projectPanel.getProjectTree().getMainTree() != null;
        e.getPresentation().setEnabled(hasTree);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
