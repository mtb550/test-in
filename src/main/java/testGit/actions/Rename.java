package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.Directory;
import testGit.pojo.TestCasesDirectory;
import testGit.pojo.TestProject;
import testGit.pojo.TestRunsDirectory;
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
        this.registerCustomShortcutSet(KeyboardSet.RenameNode.getShortcut(), tree);
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent e) {
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

        if (!(node.getUserObject() instanceof Directory dir)) return;
        if (dir instanceof TestCasesDirectory || dir instanceof TestRunsDirectory) return;

        String newName = Messages.showInputDialog("Enter new name:", "Rename", AllIcons.Actions.Edit, dir.getName(), null);
        if (newName == null || newName.isBlank() || newName.equals(dir.getName())) return;

        Tools.closeEditor(dir.getName());

        String newFileName = dir.getName().replace(dir.getName(), newName);

        TreeUtilImpl.executeVfsAction(dir.getPath(), "Rename Failed", vf -> {
            vf.rename(this, newFileName);

            Path newFilePath = dir.getPath().getParent().resolve(newFileName);
            dir.setName(newName)
                    .setName(newFileName)
                    .setPath(newFilePath)
                    .setModifiedAt(LocalDateTime.now())
                    .setModifiedBy("Muteb almughyiri");

            ((DefaultTreeModel) tree.getModel()).nodeChanged(node);

            if (dir instanceof TestProject && projectPanel.getTestProjectSelector() != null) {
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
                node.getUserObject() instanceof Directory dir &&
                !(dir instanceof TestCasesDirectory) &&
                !(dir instanceof TestRunsDirectory)
        );

        e.getPresentation().setEnabled(shouldEnable);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}