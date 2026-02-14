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
import testGit.ui.AddNewTestProjectDialog;
import testGit.util.Notifier;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.IOException;

public class AddTestProject extends AnAction {
    public final ProjectPanel projectPanel;
    public SimpleTree tree;

    public AddTestProject(ProjectPanel projectPanel) {
        super("New Test Project", "Create a new test project", AllIcons.General.Add);
        this.projectPanel = projectPanel;
        this.tree = projectPanel.getTestCaseTree();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String name = AddNewTestProjectDialog.show();

        if (name == null) return;

        Directory newProject = new Directory()
                .setType(DirectoryType.P)
                .setName(name)
                .setActive(1);

        String folderName = String.format("%s_%s_%d", newProject.getType().name().toLowerCase(), newProject.getName(), newProject.getActive());
        java.nio.file.Path projectPath = Config.getRootFolderFile().toPath().resolve(folderName);

        newProject.setFileName(folderName)
                .setFilePath(projectPath)
                .setFile(projectPath.toFile());

        WriteAction.run(() -> {
            try {
                VirtualFile rootVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(Config.getRootFolderFile());

                if (rootVf != null) {
                    VirtualFile projectDir = rootVf.createChildDirectory(this, folderName);

                    projectDir.createChildDirectory(this, "testCases");
                    projectDir.createChildDirectory(this, "testPlans");

                    projectPanel.getProjectSelector().addAndSelectProject(newProject);

                    DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                    DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) model.getRoot();
                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newProject);

                    model.insertNodeInto(newNode, rootNode, rootNode.getChildCount());

                    TreePath newPath = new TreePath(newNode.getPath());
                    tree.scrollPathToVisible(newPath);
                    tree.setSelectionPath(newPath);

                    Notifier.information("New Test Project", String.format("Test Project %s has been added", name));

                }
            } catch (IOException ex) {
                Messages.showErrorDialog("Error creating project structure: " + ex.getMessage(), "IO Error");
            }
        });
    }
}