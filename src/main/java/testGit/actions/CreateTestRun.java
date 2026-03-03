package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.testRunEditor.NewTestRunDialog;
import testGit.editorPanel.testRunEditor.TestRunEditor;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;
import testGit.pojo.TestRun;
import testGit.projectPanel.ProjectPanel;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class CreateTestRun extends DumbAwareAction {
    private final ProjectPanel projectPanel;
    private final SimpleTree testRunTree;

    public CreateTestRun(final ProjectPanel projectPanel) {
        super("New Test Run", "Create a new test run", AllIcons.Actions.GroupBy);
        this.projectPanel = projectPanel;
        this.testRunTree = projectPanel.getTestRunTree();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TreePath path = testRunTree.getSelectionPath();
        if (path == null) {
            System.out.println("path is null !!");
            return;
        }

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof Directory treeItem) || treeItem.getType() == DirectoryType.TR) {
            System.out.println("!(userObject instanceof Directory treeItem) || treeItem.getType() == DirectoryType.TR");
            return;
        }

        /*TestRunEditor.open(treeItem.getFilePath(), projectPanel, parentNode);*/
        NewTestRunDialog dialog = new NewTestRunDialog();
        if (dialog.showAndGet()) {
            TestRun metadata = dialog.getMetadata();
            TestRunEditor.create(treeItem.getFilePath(), projectPanel, parentNode, metadata);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        TreePath path = testRunTree.getSelectionPath();

        boolean isTestRun = (path != null &&
                path.getLastPathComponent() instanceof DefaultMutableTreeNode node &&
                node.getUserObject() instanceof Directory item &&
                item.getType() == DirectoryType.TR);

        e.getPresentation().setVisible(true);
        e.getPresentation().setEnabled(!isTestRun);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

}