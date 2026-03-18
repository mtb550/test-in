package testGit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.dirs.DirectoryDto;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;


public class Disable extends DumbAwareAction {
    private final SimpleTree tree;

    public Disable(final SimpleTree tree) {
        super("✏️ Rename");
        this.tree = tree;
    }

    /// To Be updated. Disable Action for projects - Explorer, Test Cases
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();
        if (!(userObject instanceof DirectoryDto treeItem)) return;

        String newName = Messages.showInputDialog("Rename node:", "Rename", null, treeItem.getName(), null);
        if (newName == null || newName.isBlank()) return;

        treeItem.setName(newName);
        ((DefaultTreeModel) tree.getModel()).nodeChanged(node);
    }
}
