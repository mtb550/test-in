package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.DirectoryType;
import testGit.pojo.TestPackage;
import testGit.util.Tools;
import testGit.util.TreeUtilImpl;

import javax.swing.tree.DefaultMutableTreeNode;

import static testGit.util.KeyboardSet.DeletePackage;

public class Remove extends DumbAwareAction {
    private final SimpleTree tree;

    public Remove(SimpleTree tree) {
        super("Remove", "Remove selected node", AllIcons.Actions.GC);
        this.tree = tree;
        this.registerCustomShortcutSet(DeletePackage.get(), tree);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

        if (node != null && node.getUserObject() instanceof TestPackage pkg) {

            if (pkg.getType() == DirectoryType.PR ||
                    pkg.getType() == DirectoryType.TCP ||
                    pkg.getType() == DirectoryType.TRP) {
                return;
            }

            int confirm = Messages.showYesNoDialog(
                    "Are you sure you want to remove '" + pkg.getName() + "'?",
                    "Confirm Removing",
                    Messages.getQuestionIcon()
            );

            if (confirm == Messages.YES) {
                System.out.println("Removing node: " + pkg.getName());

                if (pkg.getType() == DirectoryType.TS || pkg.getType() == DirectoryType.TR)
                    Tools.closeEditor(pkg.getName());

                TreeUtilImpl.removeVf(this, pkg.getFile());
                TreeUtilImpl.removeNode(node, tree);
            }
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

        boolean shouldEnable = (node != null &&
                node.getUserObject() instanceof TestPackage pkg &&
                pkg.getType() != DirectoryType.PR &&
                pkg.getType() != DirectoryType.TCP &&
                pkg.getType() != DirectoryType.TRP
        );

        e.getPresentation().setEnabled(shouldEnable);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}