package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.testCaseEditor.TestEditor;
import testGit.editorPanel.testRunEditor.RunEditor;
import testGit.pojo.DirectoryType;
import testGit.pojo.TestRunStatus;
import testGit.pojo.mappers.TestRun;
import testGit.pojo.tree.dirs.*;
import testGit.projectPanel.ProjectPanel;
import testGit.ui.CreateNodesDialog;
import testGit.ui.DirectoryOptions;
import testGit.util.KeyboardSet;
import testGit.util.TreeUtilImpl;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.nio.file.Path;

public class CreateTreeNode extends DumbAwareAction {
    private final ProjectPanel projectPanel;
    private final SimpleTree tree;
    private final DirectoryOptions option;

    public CreateTreeNode(ProjectPanel projectPanel, SimpleTree tree) {
        super("Create Node", "Create new node", AllIcons.General.Add);
        this.projectPanel = projectPanel;
        this.tree = tree;
        this.registerCustomShortcutSet(KeyboardSet.CreateNode.getShortcut(), tree);
        this.option = new DirectoryOptions();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();

        if (!(parentNode.getUserObject() instanceof Directory parentDir)) {
            return;
        }

        CreateNodesDialog.show("Node Name", option.getItems(), option.getDisabledPredicate(), (enteredName, selectedClass) -> {
            if (enteredName == null || enteredName.isEmpty()) return;

            Path newDirPath = parentDir.getPath().resolve(enteredName);

            switch (selectedClass) {
                case Class<?> c when c == TestSetPackageDirectory.class ->
                        createTestSetPackage(enteredName, parentNode, parentDir, newDirPath);

                case Class<?> c when c == TestRunPackageDirectory.class ->
                        createTestRunPackage(enteredName, parentNode, parentDir, newDirPath);

                case Class<?> c when c == TestSetDirectory.class ->
                        createTestSet(enteredName, parentNode, parentDir, newDirPath);

                case Class<?> c when c == TestRunDirectory.class -> createTestRun(enteredName, parentDir, newDirPath);

                case Class<?> c when c == TestProjectDirectory.class ->
                        System.out.println("create project from here to be implemented");

                default -> System.out.println("Unknown or null class selected: " + selectedClass);
            }

        });
    }

    private void createTestRun(String name, Directory parentDir, Path newDirPath) {
        ///  use name that you recieve
        TestRun metadata = new TestRun();
        metadata.setStatus(TestRunStatus.CREATED);

        TestRunDirectory newTestRunDirectory = new TestRunDirectory()
                .setName(name)
                .setPath(newDirPath);

        RunEditor.create(
                // attribute name not used!! to be use it and pass it here.
                newTestRunDirectory,
                projectPanel,
                projectPanel.getTestProjectSelector().getSelectedTestProject().getItem(),
                metadata
        );
        TreeUtilImpl.createDataVf(this, parentDir.getPath(), DirectoryType.TR.getMarker());
    }

    private void createTestSet(String name, DefaultMutableTreeNode parentNode, Directory parentDir, Path newDirPath) {
        TestSetDirectory newTestSetDirectory = new TestSetDirectory()
                .setName(name)
                .setPath(parentDir.getPath().resolve(name));

        TreeUtilImpl.insertVf(this, parentDir.getPath(), newTestSetDirectory.getName());
        TreeUtilImpl.createDataVf(this, newDirPath, DirectoryType.TS.getMarker());
        TreeUtilImpl.insertNode(tree, parentNode, newTestSetDirectory);
        TestEditor.open(newTestSetDirectory);
    }

    private void createTestSetPackage(String name, DefaultMutableTreeNode parentNode, Directory parentDir, Path newDirPath) {
        TestSetPackageDirectory newTestSetPackageDirectory = new TestSetPackageDirectory()
                .setName(name)
                .setPath(parentDir.getPath().resolve(name));

        TreeUtilImpl.insertVf(this, parentDir.getPath(), name);
        TreeUtilImpl.insertNode(tree, parentNode, newTestSetPackageDirectory);
        TreeUtilImpl.createDataVf(this, newDirPath, DirectoryType.TSP.getMarker());
    }

    private void createTestRunPackage(String name, DefaultMutableTreeNode parentNode, Directory parentDir, Path newDirPath) {
        TestRunPackageDirectory newTestRunPackageDirectory = new TestRunPackageDirectory()
                .setName(name)
                .setPath(parentDir.getPath().resolve(name));

        TreeUtilImpl.insertVf(this, parentDir.getPath(), name);
        TreeUtilImpl.insertNode(tree, parentNode, newTestRunPackageDirectory);
        TreeUtilImpl.createDataVf(this, newDirPath, DirectoryType.TRP.getMarker());
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        if (path == null) return;

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        switch (userObject) {
            case TestSetPackageDirectory ignored ->
                    option.type(DirectoryType.TP).setInactive().type(DirectoryType.TSP).setActive()
                            .type(DirectoryType.TRP).setInactive().type(DirectoryType.TS).setActive()
                            .type(DirectoryType.TR).setInactive();

            case TestRunPackageDirectory ignored ->
                    option.type(DirectoryType.TP).setInactive().type(DirectoryType.TSP).setInactive()
                            .type(DirectoryType.TRP).setActive().type(DirectoryType.TS).setInactive()
                            .type(DirectoryType.TR).setActive();

            case TestCasesDirectory ignored ->
                    option.type(DirectoryType.TP).setActive().type(DirectoryType.TSP).setActive()
                            .type(DirectoryType.TRP).setInactive().type(DirectoryType.TS).setActive()
                            .type(DirectoryType.TR).setInactive();

            case TestRunsDirectory ignored ->
                    option.type(DirectoryType.TP).setActive().type(DirectoryType.TSP).setInactive()
                            .type(DirectoryType.TRP).setActive().type(DirectoryType.TS).setInactive()
                            .type(DirectoryType.TR).setActive();

            default -> {
                e.getPresentation().setVisible(true);
                e.getPresentation().setEnabled(false);
            }

        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}