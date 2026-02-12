package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Directory;
import testGit.projectPanel.ProjectPanel;
import testGit.projectPanel.ProjectSelector;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.File;

public class Delete extends AnAction {
    private final ProjectPanel projectPanel;
    private final SimpleTree tree;

    public Delete(final ProjectPanel projectPanel, final SimpleTree tree) {
        super("Delete", "Delete selected node", AllIcons.Actions.GC);
        this.projectPanel = projectPanel;
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (!(selectedNode.getUserObject() instanceof Directory treeItem)) return;

        int confirm = Messages.showYesNoDialog(
                "Are you sure you want to delete '" + treeItem.getName() + "' and all its folders/files on disk?",
                "Confirm Delete",
                Messages.getQuestionIcon()
        );
        if (confirm != Messages.YES) return;

        try {
            File fileOnDisk = treeItem.getFilePath().toFile();
            if (fileOnDisk.exists()) {
                boolean success = FileUtil.delete(fileOnDisk);

                if (success) {
                    System.out.println("Success! Deleted: " + treeItem.getFilePath());
                    //refreshPath(treeItem.getFilePath().getParent());

                } else {
                    System.out.println("Could not delete folder. It might be in use. Delete Failed.");
                    return;
                }
            }

            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.removeNodeFromParent(selectedNode);

            // بعد نجاح الحذف، حدث الـ ComboBox بسهولة
            if (projectPanel.getProjectSelector() != null) {
                projectPanel.getProjectSelector().loadProjectList();
                //panel.loadAllProjects();
                projectPanel.filterByProject(ProjectSelector.comboBox.getItem());
            }

        } catch (Exception ex) {
            //Messages.showErrorDialog("Error during delete: " + ex.getMessage(), "Error");
            System.out.println("Error during delete: " + ex.getMessage());
        }
    }
}