package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
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

public class RenameAction extends AnAction {
    private final ProjectPanel projectPanel;
    private final SimpleTree tree;

    // استقبال الـ panel عبر الـ Constructor كما فعلنا في DeleteAction
    public RenameAction(final ProjectPanel projectPanel, final SimpleTree tree) {
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

        // 1. طلب الاسم الجديد من المستخدم
        String newName = Messages.showInputDialog("Enter new name:", "Rename", null, treeItem.getName(), null);
        if (newName == null || newName.isBlank() || newName.equals(treeItem.getName())) return;

        try {
            File oldFile = treeItem.getFilePath().toFile();
            String newFileName = treeItem.getType() + "_" + newName + "_" + treeItem.getActive();
            Path newFilePath = treeItem.getFilePath().getParent().resolve(newFileName);
            File newFile = newFilePath.toFile();

            if (oldFile.renameTo(newFile)) {
                // 3. تحديث بيانات الكائن (Object)
                treeItem.setName(newName);
                treeItem.setFileName(newFileName);
                treeItem.setFilePath(newFilePath);

                // 4. تحديث الواجهة (Tree & VFS)
                ((DefaultTreeModel) tree.getModel()).nodeChanged(node);
                //refreshPath(newFilePath);
                //refreshPath(newFilePath.getParent());

                // 5. تحديث الـ ComboBox إذا كان العنصر "مشروع" (Type 0)
                if (treeItem.getType() == DirectoryType.P && projectPanel.getProjectSelector() != null) {
                    projectPanel.getProjectSelector().loadProjectList();
                }

                System.out.println("Success! Renamed to: " + newFileName);
            } else {
                Messages.showErrorDialog("Could not rename folder. Make sure it's not open in another program.", "Rename Failed");
            }

        } catch (Exception ex) {
            System.err.println("Error during rename: " + ex.getMessage());
        }
    }
}