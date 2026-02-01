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
import testGit.projectPanel.ProjectPanel;
import testGit.util.NodeType;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.File;
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
        System.out.println("AddProjectAction.actionPerformed()");
        String name = Messages.showInputDialog("Enter project name:", "Add New Project", null);
        if (name == null || name.isBlank()) return;

        if (tree == null) {
            System.err.println("AddProjectAction.actionPerformed(): tree is null");
            return;
        }

        TreePath path = tree.getSelectionPath();
        DefaultMutableTreeNode parentNode;
        if (path == null) {
            System.out.println("AddProjectAction.actionPerformed(): path is null");
            parentNode = (DefaultMutableTreeNode) tree.getModel().getRoot();
        } else {
            parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        }

        Directory newProject = new Directory()
                .setType(NodeType.PROJECT.getCode())
                .setId(100)
                .setName(name)
                .setActive(1);

        newProject.setFileName(String.format("%d_%d_%s_%d", newProject.getType(), newProject.getId(), newProject.getName(), newProject.getActive()));
        newProject.setFilePath(Config.getRootFolder().toPath().resolve(newProject.getFileName()));
        newProject.setFile(new File(newProject.getFileName()));


        WriteAction.run(() -> {
            try {
                // 1. Get the parent directory as a VirtualFile
                File rootFolder = Config.getRootFolder();
                System.out.println(rootFolder.getPath());
                VirtualFile parentDir = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(rootFolder.toPath());
                System.out.println("parentDir: " + parentDir);

                if (parentDir != null) {
                    parentDir.createChildDirectory(this, newProject.getFileName());
                    projectPanel.getProjectSelector().addAndSelectProject(newProject);

                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newProject);
                    DefaultTreeModel model = (DefaultTreeModel) projectPanel.getTestCaseTree().getModel();
                    model.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
                    tree.scrollPathToVisible(new TreePath(newNode.getPath()));

                    tree.scrollPathToVisible(new TreePath(newNode.getPath()));
                }
            } catch (IOException ex) {
                System.err.println("unable to create project: " + newProject);
            }
        });
    }
}