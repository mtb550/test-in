package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Directory;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;


public class Enable extends DumbAwareAction {
    private final SimpleTree tree;

    public Enable(final SimpleTree tree) {
        super("Rename", "", AllIcons.Actions.Edit);
        this.tree = tree;
    }

    /// To Be updated. Enable Action for projects - Explorer, Test Cases
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();
        if (!(userObject instanceof Directory treeItem)) return;

        String newName = Messages.showInputDialog("Rename node:", "Rename", null, treeItem.getName(), null);
        if (newName == null || newName.isBlank()) return;

        treeItem.setName(newName);
        ((DefaultTreeModel) tree.getModel()).nodeChanged(node);
    }
}
