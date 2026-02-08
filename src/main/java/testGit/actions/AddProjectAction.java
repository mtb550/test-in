package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;
import testGit.projectPanel.ProjectPanel;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.IOException;

public class AddProjectAction extends AnAction {
    public final ProjectPanel projectPanel;
    public SimpleTree tree;

    public AddProjectAction(ProjectPanel projectPanel) {
        super("➕ New Project", "Add new project", AllIcons.General.Add);
        this.projectPanel = projectPanel;
        this.tree = projectPanel.getTestCaseTree();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String name = Messages.showInputDialog("Enter project name:", "Add New Project", null);
        if (name == null || name.isBlank()) return;

        // 1. إعداد بيانات المشروع الجديد
        Directory newProject = new Directory()
                .setType(DirectoryType.P)
                .setName(name)
                .setActive(1);

        // استخدام name().toLowerCase() لضمان الاتساق
        String folderName = String.format("%s_%s_%d", newProject.getType().name().toLowerCase(), newProject.getName(), newProject.getActive());
        java.nio.file.Path projectPath = Config.getRootFolderFile().toPath().resolve(folderName);

        newProject.setFileName(folderName)
                .setFilePath(projectPath)
                .setFile(projectPath.toFile());

        WriteAction.run(() -> {
            try {
                VirtualFile rootVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(Config.getRootFolderFile());

                if (rootVf != null) {
                    // 2. إنشاء مجلد المشروع الرئيسي
                    VirtualFile projectDir = rootVf.createChildDirectory(this, folderName);

                    // 3. إنشاء المجلدات الفرعية الإلزامية داخل المشروع الجديد
                    projectDir.createChildDirectory(this, "testCases");
                    projectDir.createChildDirectory(this, "testPlans");

                    // 4. تحديث الـ ComboBox واختيار المشروع الجديد
                    projectPanel.getProjectSelector().addAndSelectProject(newProject);

                    // 5. تحديث الشجرة (Tree UI)
                    DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                    DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) model.getRoot();
                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newProject);

                    model.insertNodeInto(newNode, rootNode, rootNode.getChildCount());

                    // التمرير للعنصر الجديد واختياره
                    TreePath newPath = new TreePath(newNode.getPath());
                    tree.scrollPathToVisible(newPath);
                    tree.setSelectionPath(newPath);
                }
            } catch (IOException ex) {
                Messages.showErrorDialog("Error creating project structure: " + ex.getMessage(), "IO Error");
            }
        });
    }
}