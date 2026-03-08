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
import testGit.pojo.DirectoryStatus;
import testGit.pojo.DirectoryType;
import testGit.projectPanel.ProjectPanel;
import testGit.ui.InputDialogList;
import testGit.ui.InputDialogList_TestRun;
import testGit.util.Notifier;
import testGit.util.TreeUtilImpl;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.nio.file.Path;

public class CreateTestRunItems extends DumbAwareAction {
    private final SimpleTree tree;
    private final ProjectPanel projectPanel;

    public CreateTestRunItems(ProjectPanel projectPanel, SimpleTree tree) {
        super("New Item", "Create a new item", AllIcons.Nodes.Package);
        this.projectPanel = projectPanel;
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent e) {
        System.out.println("CreateTestRunItems.actionPerformed()");

        TreePath path = tree.getSelectionPath();
        if (path == null) {
            System.out.println("path is null !!");

            Directory selectedTestProject = projectPanel.getTestProjectSelector().getSelectedTestProject().getItem();


            InputDialogList_TestRun.show("Test Project Name", (enteredName, selectedItem) -> {
                System.out.println("Processing: " + enteredName + " | Selected Type: " + selectedItem.name());
                if (enteredName != null && !enteredName.isEmpty()) {
                    add_new(selectedTestProject, enteredName ,selectedItem);
                }
            });

            return;
        }

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof Directory treeItem) || treeItem.getType() == DirectoryType.TR) return;

        String name = Messages.showInputDialog("Enter package name:", "Create Package", AllIcons.Nodes.Package);
        if (name == null || name.isBlank()) return;
        name = name.replace("_", " ");

        Path parentPath = (treeItem.getType() == DirectoryType.PR)
                ? treeItem.getFilePath().resolve("testRuns")
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

    private void add_new(Directory selectedTestProject, String enteredName, InputDialogList_TestRun.TemplateItem selectedItem) {
        Path parentPath = selectedTestProject.getFilePath().resolve("testRuns");

        if (selectedItem.type() == DirectoryType.PA) {
            Directory newPackage = new Directory()
                    .setType(DirectoryType.PA)
                    .setName(enteredName)
                    .setStatus(DirectoryStatus.AC);

            String folderName = String.format("%s_%s_%s", newPackage.getType().name(), newPackage.getName(), newPackage.getStatus());
            Path fullPath = parentPath.resolve(folderName);

            newPackage.setFileName(folderName)
                    .setFilePath(fullPath)
                    .setFile(fullPath.toFile());

            TreeUtilImpl.insertVf(this, parentPath, folderName);
            TreeUtilImpl.insertNode(tree, projectPanel.getTestCaseTabController().getRootNode(), newPackage);
            Notifier.info("Test Package Created", "Create new test package under: " + parentPath);
        }

        else if (selectedItem.type() == DirectoryType.TR) {
            Directory newTestRun = new Directory()
                    .setType(DirectoryType.TR)
                    .setName(enteredName)
                    .setStatus(DirectoryStatus.AC);

            String folderName = String.format("%s_%s_%s", newTestRun.getType().name(), newTestRun.getName(), newTestRun.getStatus());
            Path fullPath = parentPath.resolve(folderName);

            newTestRun.setFileName(folderName)
                    .setFilePath(fullPath)
                    .setFile(fullPath.toFile());

            TreeUtilImpl.insertVf(this, parentPath, folderName);
            TreeUtilImpl.insertNode(tree, projectPanel.getTestCaseTabController().getRootNode(), newTestRun);
            Notifier.info("Test Run Created", "Create new test run under: " + parentPath);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        boolean isTestRun = (path != null &&
                path.getLastPathComponent() instanceof DefaultMutableTreeNode node &&
                node.getUserObject() instanceof Directory item &&
                (item.getType() == DirectoryType.PA || item.getType() == DirectoryType.TR));

        e.getPresentation().setVisible(true);
        e.getPresentation().setEnabled(!isTestRun);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

}
