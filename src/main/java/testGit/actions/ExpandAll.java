package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;

public class ExpandAll extends DumbAwareAction {
    private final SimpleTree tree;

    public ExpandAll(SimpleTree tree) {
        super("Expand All", "Expand all nodes", AllIcons.Actions.Expandall);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        for (int i = 0; i < tree.getRowCount(); i++)
            tree.expandRow(i);
    }
}
