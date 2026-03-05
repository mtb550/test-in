package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryStatus;
import testGit.pojo.DirectoryType;
import testGit.projectPanel.ProjectPanel;
import testGit.ui.AddNewTestPackageDialog;
import testGit.util.Notifier;
import testGit.util.TreeUtilImpl;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.nio.file.Path;

public class CreateTestCasePackage extends DumbAwareAction {
    private final ProjectPanel projectPanel;
    private final SimpleTree tree;

    public CreateTestCasePackage(ProjectPanel projectPanel, SimpleTree tree) {
        super("New Package", "Create a new package", AllIcons.Nodes.Package);
        this.projectPanel = projectPanel;
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent e) {
        System.out.println("CreateTestCasePackage.actionPerformed()");

        TreePath path = tree.getSelectionPath();
        if (path == null) {
            System.out.println("path is null !!, first case package");
            Directory selectedTestProject = projectPanel.getTestProjectSelector().getSelectedTestProject().getItem();

            String name = AddNewTestPackageDialog.show();

            Path parentPath = selectedTestProject.getFilePath().resolve("testCases");

            Directory newPackage = new Directory()
                    .setType(DirectoryType.PA)
                    .setName(name)
                    .setStatus(DirectoryStatus.AC);

            String folderName = String.format("%s_%s_%s", newPackage.getType().name(), newPackage.getName(), newPackage.getStatus());
            Path fullPath = parentPath.resolve(folderName);

            newPackage.setFileName(folderName)
                    .setFilePath(fullPath)
                    .setFile(fullPath.toFile());

            TreeUtilImpl.insertVf(this, parentPath, folderName);
            TreeUtilImpl.insertNode(tree, projectPanel.getTestCaseTabController().getRootNode(), newPackage);
            Notifier.info("Test Package Created", "Create new test package under: " + parentPath);
            return;
        }

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof Directory treeItem) || treeItem.getType() == DirectoryType.TS) return;

        String name = AddNewTestPackageDialog.show();

        Path parentPath = (treeItem.getType() == DirectoryType.PR)
                ? treeItem.getFilePath().resolve("testCases")
                : treeItem.getFilePath();

        Directory newPackage = new Directory()
                .setType(DirectoryType.PA)
                .setName(name)
                .setStatus(DirectoryStatus.AC);

        String folderName = String.format("%s_%s_%s", newPackage.getType().name(), newPackage.getName(), newPackage.getStatus());
        Path fullPath = parentPath.resolve(folderName);

        newPackage.setFileName(folderName)
                .setFilePath(fullPath)
                .setFile(fullPath.toFile());

        TreeUtilImpl.insertVf(this, parentPath, folderName);
        TreeUtilImpl.insertNode(tree, parentNode, newPackage);

    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        boolean isFeature = (path != null &&
                path.getLastPathComponent() instanceof DefaultMutableTreeNode node &&
                node.getUserObject() instanceof Directory item &&
                item.getType() == DirectoryType.TS);

        e.getPresentation().setVisible(true);
        e.getPresentation().setEnabled(!isFeature);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}