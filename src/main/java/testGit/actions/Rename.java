package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.dto.dirs.DirectoryDto;
import testGit.pojo.dto.dirs.TestCasesDirectoryDto;
import testGit.pojo.dto.dirs.TestProjectDirectoryDto;
import testGit.pojo.dto.dirs.TestRunsDirectoryDto;
import testGit.projectPanel.ProjectPanel;
import testGit.util.KeyboardSet;
import testGit.util.Tools;
import testGit.util.TreeUtilImpl;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Rename extends DumbAwareAction {
    private final ProjectPanel projectPanel;
    private final SimpleTree tree;

    public Rename(final ProjectPanel projectPanel, final SimpleTree tree) {
        super("Rename", "Rename selected node", AllIcons.Actions.Edit);
        this.projectPanel = projectPanel;
        this.tree = tree;
        this.registerCustomShortcutSet(KeyboardSet.RenameNode.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent e) {
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

        if (!(node.getUserObject() instanceof DirectoryDto dir)) return;
        if (dir instanceof TestCasesDirectoryDto || dir instanceof TestRunsDirectoryDto) return;

        String newName = Messages.showInputDialog("Enter new name:", "Rename", AllIcons.Actions.Edit, dir.getName(), null);
        if (newName == null || newName.isBlank() || newName.equals(dir.getName())) return;

        Tools.closeEditor(dir.getName());

        Path oldPath = dir.getPath();
        Path newPath = oldPath.getParent().resolve(newName);

        TreeUtilImpl.executeVfsAction(oldPath, "Rename Failed", vf -> {
            vf.rename(this, newName);

            dir.setName(newName)
                    .setName(newName)
                    .setPath(newPath)
                    .setModifiedAt(LocalDateTime.now())
                    .setModifiedBy("Muteb almughyiri");

            Tools.updateChildrenPathsRecursive(node, oldPath, newPath);
            ((DefaultTreeModel) tree.getModel()).nodeChanged(node);

            if (dir instanceof TestProjectDirectoryDto && projectPanel.getTestProjectSelector() != null) {
                projectPanel.getTestProjectSelector().loadTestProjectList();
            }

            System.out.println("Success! Renamed to: " + newName);
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        boolean shouldEnable = (path != null &&
                path.getLastPathComponent() instanceof DefaultMutableTreeNode node &&
                node.getUserObject() instanceof DirectoryDto dir &&
                !(dir instanceof TestCasesDirectoryDto) &&
                !(dir instanceof TestRunsDirectoryDto)
        );

        e.getPresentation().setEnabled(shouldEnable);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }


}