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
import org.jetbrains.annotations.Nullable;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;
import testGit.projectPanel.ProjectPanel;
import testGit.util.KeyboardSet;
import testGit.util.Tools;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Rename extends DumbAwareAction {
    private final ProjectPanel projectPanel;
    private final SimpleTree tree;

    public Rename(final ProjectPanel projectPanel, final SimpleTree tree) {
        super("Rename", "Rename selected node", AllIcons.Actions.Edit);
        this.projectPanel = projectPanel;
        this.tree = tree;
        this.registerCustomShortcutSet(KeyboardSet.Rename.get(), tree);
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent e) {
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

        if (!(node.getUserObject() instanceof Directory dir)) return;
        if (dir.getType() == DirectoryType.TCP || dir.getType() == DirectoryType.TRP) return;

        String newName = Messages.showInputDialog("Enter new name:", "Rename", AllIcons.Actions.Edit, dir.getName(), null);
        if (newName == null || newName.isBlank() || newName.equals(dir.getName())) return;

        Tools.closeEditor(dir.getName());

        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(dir.getFile());

        if (vf != null) {
            String newFileName = dir.getFileName().replace(dir.getName(), newName);

            WriteAction.run(() -> {
                try {
                    vf.rename(this, newFileName);

                    Path newFilePath = dir.getFilePath().getParent().resolve(newFileName);
                    dir.setName(newName)
                            .setFileName(newFileName)
                            .setFilePath(newFilePath)
                            .setFile(newFilePath.toFile())
                            .setModifiedAt(LocalDateTime.now())
                            .setModifiedBy("Muteb almughyiri");

                    ((DefaultTreeModel) tree.getModel()).nodeChanged(node);

                    if (dir.getType() == DirectoryType.PR && projectPanel.getTestProjectSelector() != null) {
                        projectPanel.getTestProjectSelector().loadTestProjectList();
                    }

                    System.out.println("Success! Renamed to: " + newName);

                } catch (IOException ex) {
                    Messages.showErrorDialog("Could not rename folder. Make sure it's not open in another program.\n" + ex.getMessage(), "Rename Failed");
                }
            });
        } else {
            Messages.showErrorDialog("Could not find the original file on disk.", "Rename Failed");
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        boolean shouldEnable = (path != null &&
                path.getLastPathComponent() instanceof DefaultMutableTreeNode node &&
                node.getUserObject() instanceof Directory dir &&
                dir.getType() != DirectoryType.TCP &&
                dir.getType() != DirectoryType.TRP
        );

        e.getPresentation().setEnabled(shouldEnable);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}