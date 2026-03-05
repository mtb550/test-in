package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryStatus;
import testGit.pojo.DirectoryType;
import testGit.util.TreeUtilImpl;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.nio.file.Path;

public class CreateTestRunPackage extends DumbAwareAction {
    private final SimpleTree tree;

    public CreateTestRunPackage(final SimpleTree tree) {
        super("New Package", "Create a new package", AllIcons.Nodes.Package);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("CreateTestRunPackage.actionPerformed()");

        TreePath path = tree.getSelectionPath();
        if (path == null) {
            System.out.println("path is null !!");
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
