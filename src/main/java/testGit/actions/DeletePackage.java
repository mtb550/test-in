package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Directory;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.IOException;

import static testGit.util.ShortcutSet.DeletePackage;

public class DeletePackage extends DumbAwareAction {
    private final SimpleTree tree;

    public DeletePackage(final SimpleTree tree) {
        super("Delete", "Delete selected node", AllIcons.Actions.GC);
        this.tree = tree;
        this.registerCustomShortcutSet(DeletePackage.get(), tree);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (!(node.getUserObject() instanceof Directory treeItem)) return;

        int confirm = Messages.showYesNoDialog(
                "Are you sure you want to delete '" + treeItem.getName() + "'?",
                "Confirm Delete",
                Messages.getQuestionIcon()
        );

        if (confirm == Messages.YES)
            performDeletion(node, treeItem);

    }

    private void performDeletion(DefaultMutableTreeNode node, Directory treeItem) {
        WriteAction.run(() -> {
            try {
                // Use LocalFileSystem to safely delete the file/directory
                VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(treeItem.getFile());
                if (vf != null) {
                    vf.delete(this);
                }

                // Update the tree model directly from the projectPanel
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                model.removeNodeFromParent(node);
            } catch (IOException ex) {
                Messages.showErrorDialog("Could not delete file: " + ex.getMessage(), "Error");
            }
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        e.getPresentation().setEnabled(node != null && node.getUserObject() instanceof Directory);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}