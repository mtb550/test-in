package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.testCaseEditor.TestCaseEditor;
import testGit.editorPanel.testRunEditor.TestRunEditor;
import testGit.pojo.*;
import testGit.projectPanel.ProjectPanel;
import testGit.ui.CreateNodesDialog;
import testGit.ui.DirectoryOptions;
import testGit.util.KeyboardSet;
import testGit.util.TreeUtilImpl;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.nio.file.Path;

public class CreateNode extends DumbAwareAction {
    private final ProjectPanel projectPanel;
    private final SimpleTree tree;
    DirectoryOptions option;

    public CreateNode(ProjectPanel projectPanel, SimpleTree tree) {
        super("Create Node", "Create new node", AllIcons.General.Add);
        this.projectPanel = projectPanel;
        this.tree = tree;
        this.registerCustomShortcutSet(KeyboardSet.CreateNode.getShortcut(), tree);

        option = new DirectoryOptions()
                .type(DirectoryType.PR).setActive()
                .type(DirectoryType.PA).setActive()
                .type(DirectoryType.TS).setActive()
                .type(DirectoryType.TR).setActive();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("Add new node triggered via AddNewNodeAction");

        CreateNodesDialog.show("Test Project Name", option.getItems(), option.getDisabledPredicate(), (enteredName, selectedItem) -> {
            if (enteredName == null || enteredName.isEmpty()) return;

            /// use switch instead of if-statements
            if (selectedItem == DirectoryType.PA) {
                createTestPackage(enteredName);
                return;
            }

            if (selectedItem == DirectoryType.TS) {
                createTestSet(enteredName);
                return;
            }

            if (selectedItem == DirectoryType.TR) {
                createTestRun(enteredName);
                return;
            }

            if (selectedItem == DirectoryType.PR) {
                System.out.println("create project from here to be implemented");
                /// to be implemented
            }

        });
    }

    private void createTestRun(String name) {
        /// to be removed and used once not in all creation methods
        TreePath path = tree.getSelectionPath();

        if (path == null) return;

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();
        /// removing to here

        if (!(userObject instanceof TestPackage pkg)) return;

        TestRun metadata = new TestRun();
        metadata.setStatus(TestRunStatus.CREATED);

        TestRunEditor.create(
                // attribute name not used!! to be use it and pass it here.
                pkg,
                projectPanel,
                projectPanel.getTestProjectSelector().getSelectedTestProject().getItem(),
                metadata
        );
    }

    private void createTestSet(String name) {
        /// to be removed and used once not in all creation methods
        TreePath path = tree.getSelectionPath();

        if (path == null) return;

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();
        /// removing to here

        if (!(userObject instanceof TestPackage pkg)) return;

        TestPackage newTestSet = new TestPackage()
                .setType(DirectoryType.TS)
                .setName(name)
                .setIcon(DirectoryIcon.TS);

        newTestSet.setFileName(String.format("%s_%s", newTestSet.getType().toString(), newTestSet.getName()));

        newTestSet.setFilePath(pkg.getFilePath().resolve(newTestSet.getFileName()));

        System.out.println("AddTestSet.actionPerformed(): newTestSet = " + newTestSet.getFilePath());
        newTestSet.setFile(newTestSet.getFilePath().toFile());

        TreeUtilImpl.insertVf(this, pkg.getFilePath(), newTestSet.getFileName());

        TreeUtilImpl.insertNode(tree, parentNode, newTestSet);
        TestCaseEditor.open(newTestSet);
    }

    private void createTestPackage(String name) {
        /// to be removed and used once not in all creation methods
        TreePath path = tree.getSelectionPath();

        if (path == null) return;

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();
        /// removing to here

        if (!(userObject instanceof TestPackage pkg)) return;

        TestPackage newTestPackage = new TestPackage()
                .setType(DirectoryType.PA)
                .setName(name);

        String folderName = String.format("%s_%s", newTestPackage.getType().name(), newTestPackage.getName());
        Path fullPath = pkg.getFilePath().resolve(folderName);

        newTestPackage.setFileName(folderName)
                .setFilePath(fullPath)
                .setFile(fullPath.toFile())
                .setIcon(DirectoryIcon.PA);

        TreeUtilImpl.insertVf(this, pkg.getFilePath(), folderName);
        TreeUtilImpl.insertNode(tree, parentNode, newTestPackage);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        if (path == null) return;

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        if (userObject instanceof TestPackage pkg && pkg.getType() == DirectoryType.PA) {

            option.type(DirectoryType.PR).setInactive()
                    .type(DirectoryType.PA).setActive()
                    .type(DirectoryType.TS).setStatus(pkg.getFilePath().toString().contains("TCP_testCases"))
                    .type(DirectoryType.TR).setStatus(pkg.getFilePath().toString().contains("TRP_testRuns"));
        }

        if (userObject instanceof TestPackage pkg && pkg.getType() == DirectoryType.TS) {
            e.getPresentation().setVisible(true);
            e.getPresentation().setEnabled(false);
            return;
        }

        if (userObject instanceof TestPackage pkg && pkg.getType() == DirectoryType.TCP) {
            option.type(DirectoryType.PR).setInactive()
                    .type(DirectoryType.PA).setActive()
                    .type(DirectoryType.TS).setActive()
                    .type(DirectoryType.TR).setInactive();
            return;
        }

        if (userObject instanceof TestPackage pkg && pkg.getType() == DirectoryType.TRP) {
            option.type(DirectoryType.PR).setInactive()
                    .type(DirectoryType.PA).setActive()
                    .type(DirectoryType.TS).setInactive()
                    .type(DirectoryType.TR).setActive();
            return;
        }

        if (userObject instanceof TestPackage pkg && pkg.getType() == DirectoryType.TR) {
            e.getPresentation().setVisible(true);
            e.getPresentation().setEnabled(false);
        }

    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}