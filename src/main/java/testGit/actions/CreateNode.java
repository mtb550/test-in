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
import testGit.pojo.mappers.TestRunJsonMapper;
import testGit.projectPanel.ProjectPanel;
import testGit.ui.CreateNodesDialog;
import testGit.ui.DirectoryOptions;
import testGit.util.KeyboardSet;
import testGit.util.TreeUtilImpl;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class CreateNode extends DumbAwareAction {
    private final ProjectPanel projectPanel;
    private final SimpleTree tree;
    private final DirectoryOptions option;

    public CreateNode(ProjectPanel projectPanel, SimpleTree tree) {
        super("Create Node", "Create new node", AllIcons.General.Add);
        this.projectPanel = projectPanel;
        this.tree = tree;
        this.registerCustomShortcutSet(KeyboardSet.CreateNode.getShortcut(), tree);
        this.option = new DirectoryOptions();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("Add new node triggered via AddNewNodeAction");

        CreateNodesDialog.show("Node Name", option.getItems(), option.getDisabledPredicate(), (enteredName, selectedClass) -> {
            if (enteredName == null || enteredName.isEmpty()) return;

            switch (selectedClass) {
                case Class<?> c when c == TestSetPackage.class -> createTestCasePackage(enteredName);

                case Class<?> c when c == TestRunPackage.class -> createTestRunPackage(enteredName);

                case Class<?> c when c == TestSet.class -> createTestSet(enteredName);

                case Class<?> c when c == TestRun.class -> createTestRun(enteredName);

                case Class<?> c when c == TestProject.class ->
                        System.out.println("create project from here to be implemented");

                default -> System.out.println("Unknown or null class selected: " + selectedClass);
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

        if (!(userObject instanceof TestRun tr)) return;

        TestRunJsonMapper metadata = new TestRunJsonMapper();
        metadata.setStatus(TestRunStatus.CREATED);

        TestRunEditor.create(
                // attribute name not used!! to be use it and pass it here.
                tr,
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

        if (!(userObject instanceof TestSet ts)) return;

        TestSet newTestSet = new TestSet()
                .setName(name)
                .setPath(ts.getPath().resolve(name));

        System.out.println("AddTestSet.actionPerformed(): newTestSet = " + newTestSet.getPath());

        TreeUtilImpl.insertVf(this, ts.getPath(), newTestSet.getName());

        TreeUtilImpl.insertNode(tree, parentNode, newTestSet);
        TestCaseEditor.open(newTestSet);
    }

    private void createTestCasePackage(String name) {
        /// to be removed and used once not in all creation methods
        TreePath path = tree.getSelectionPath();

        if (path == null) return;

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();
        /// removing to here

        if (!(userObject instanceof TestSetPackage tsp)) return;

        TestSetPackage newTestSetPackage = new TestSetPackage()
                .setName(name)
                .setPath(tsp.getPath().resolve(name));

        TreeUtilImpl.insertVf(this, tsp.getPath(), name);
        TreeUtilImpl.insertNode(tree, parentNode, newTestSetPackage);
    }

    private void createTestRunPackage(String name) {
        /// to be removed and used once not in all creation methods
        TreePath path = tree.getSelectionPath();

        if (path == null) return;

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();
        /// removing to here

        if (!(userObject instanceof TestRunPackage trp)) return;

        TestRunPackage newTestRunPackage = new TestRunPackage()
                .setName(name)
                .setPath(trp.getPath().resolve(name));

        TreeUtilImpl.insertVf(this, trp.getPath(), name);
        TreeUtilImpl.insertNode(tree, parentNode, newTestRunPackage);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        if (path == null) return;

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        if (userObject instanceof TestSetPackage) {
            option.type(DirectoryType.TP).setInactive()
                    .type(DirectoryType.TSP).setActive()
                    .type(DirectoryType.TRP).setInactive()
                    .type(DirectoryType.TS).setActive()
                    .type(DirectoryType.TR).setInactive();
        }

        if (userObject instanceof TestRunPackage) {
            option.type(DirectoryType.TP).setInactive()
                    .type(DirectoryType.TSP).setInactive()
                    .type(DirectoryType.TRP).setActive()
                    .type(DirectoryType.TS).setInactive()
                    .type(DirectoryType.TR).setActive();
        }

        if (userObject instanceof TestSet) {
            e.getPresentation().setVisible(true);
            e.getPresentation().setEnabled(false);
            return;
        }

        if (userObject instanceof TestCasesDirectory) {
            option.type(DirectoryType.TP).setInactive()
                    .type(DirectoryType.TSP).setActive()
                    .type(DirectoryType.TRP).setInactive()
                    .type(DirectoryType.TS).setActive()
                    .type(DirectoryType.TR).setInactive();
            return;
        }

        if (userObject instanceof TestRunsDirectory) {
            option.type(DirectoryType.TP).setInactive()
                    .type(DirectoryType.TSP).setInactive()
                    .type(DirectoryType.TRP).setActive()
                    .type(DirectoryType.TS).setInactive()
                    .type(DirectoryType.TR).setActive();
            return;
        }

        if (userObject instanceof TestRun) {
            e.getPresentation().setVisible(true);
            e.getPresentation().setEnabled(false);
        }

    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}