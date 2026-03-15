package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.editorPanel.testRunEditor.TestRunEditor;
import testGit.pojo.DirectoryType;
import testGit.pojo.TestPackage;
import testGit.pojo.TestRun;
import testGit.pojo.TestRunStatus;
import testGit.projectPanel.ProjectPanel;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class CreateTestRun extends DumbAwareAction {
    private final ProjectPanel projectPanel;
    private final SimpleTree tree;

    public CreateTestRun(ProjectPanel projectPanel) {
        super("New Test Run", "Create a new test run", AllIcons.Actions.GroupBy);
        this.projectPanel = projectPanel;
        this.tree = projectPanel.getProjectTree().getMainTree();
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent e) {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            System.out.println("path is null !!");
            return;
        }

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof TestPackage pkg)
                || pkg.getType() == DirectoryType.TR) {
            System.out.println("!(userObject instanceof Directory pkg) || pkg.getType() == DirectoryType.TR");
            return;
        }

        /*TestRunEditor.open(pkg.getFilePath(), projectPanel, parentNode);*/
        //NewTestRunDialog dialog = new NewTestRunDialog();
        //if (dialog.showAndGet()) {
        //TestRun metadata = dialog.getMetadata();

        TestRun metadata = new TestRun();
        metadata.setStatus(TestRunStatus.CREATED);

        TestRunEditor.create(
                pkg,
                projectPanel,
                projectPanel.getTestProjectSelector().getSelectedTestProject().getItem(),
                metadata
        );
        //}
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        boolean shouldEnable = (path != null &&
                path.getLastPathComponent() instanceof DefaultMutableTreeNode node &&
                node.getUserObject() instanceof TestPackage item &&
                item.getType() != DirectoryType.TR &&
                item.getType() != DirectoryType.TS &&
                !item.getFilePath().toString().contains("TCP_testCases")
        );

        //e.getPresentation().setVisible(true);
        e.getPresentation().setEnabled(shouldEnable);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}