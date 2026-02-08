package testGit.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;


public class RunAction extends AnAction {
    private final SimpleTree tree;

    public RunAction(final SimpleTree tree) {
        super("▶ Run Feature");
        this.tree = tree;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        if (tree == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        TreePath path = tree.getSelectionPath();
        if (path == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        e.getPresentation().setEnabled(userObject instanceof Directory treeItem && treeItem.getType() == DirectoryType.F);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: Run the feature test automation
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
