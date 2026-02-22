package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;
import testGit.projectPanel.ProjectPanel;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.File;
import java.nio.file.Path;

public class Rename extends DumbAwareAction {
    private final ProjectPanel projectPanel;
    private final SimpleTree tree;

    public Rename(final ProjectPanel projectPanel, final SimpleTree tree) {
        super("Rename", "Rename selected node", AllIcons.Actions.Edit);
        this.projectPanel = projectPanel;
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (!(node.getUserObject() instanceof Directory treeItem)) return;

        String newName = Messages.showInputDialog("Enter new name:", "Rename", null, treeItem.getName(), null);
        if (newName == null || newName.isBlank() || newName.equals(treeItem.getName())) return;

        try {
            File oldFile = treeItem.getFilePath().toFile();
            String newFileName = treeItem.getType() + "_" + newName + "_" + treeItem.getActive();
            Path newFilePath = treeItem.getFilePath().getParent().resolve(newFileName);
            File newFile = newFilePath.toFile();

            if (oldFile.renameTo(newFile)) {
                treeItem.setName(newName);
                treeItem.setFileName(newFileName);
                treeItem.setFilePath(newFilePath);

                ((DefaultTreeModel) tree.getModel()).nodeChanged(node);

                if (treeItem.getType() == DirectoryType.PR && projectPanel.getProjectSelector() != null) {
                    projectPanel.getProjectSelector().loadProjectList();
                }

                System.out.println("Success! Renamed to: " + newFileName);

                ///  update vfs after rename. to fix the issue can not find directory
            } else {
                Messages.showErrorDialog("Could not rename folder. Make sure it's not open in another program.", "Rename Failed");
            }

        } catch (Exception ex) {
            System.err.println("Error during rename: " + ex.getMessage());
        }
    }
}