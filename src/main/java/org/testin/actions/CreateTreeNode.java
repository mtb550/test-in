package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.testCaseEditor.TestEditor;
import org.testin.editorPanel.testRunEditor.RunEditor;
import org.testin.pojo.CreateNodeMenu;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.TestRunStatus;
import org.testin.pojo.dto.TestRunDto;
import org.testin.pojo.dto.dirs.*;
import org.testin.projectPanel.ProjectPanel;
import org.testin.ui.createNodes.CreateNodesDialog;
import org.testin.util.KeyboardSet;
import org.testin.util.Tools;
import org.testin.util.TreeUtilImpl;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.Arrays;

public class CreateTreeNode extends DumbAwareAction {
    private final ProjectPanel projectPanel;
    private final SimpleTree tree;

    public CreateTreeNode(final ProjectPanel projectPanel, final SimpleTree tree) {
        super("Create", "Create new node", AllIcons.General.Add);
        this.projectPanel = projectPanel;
        this.tree = tree;
        this.registerCustomShortcutSet(KeyboardSet.CreateNode.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();

        if (!(parentNode.getUserObject() instanceof DirectoryDto parentDir)) {
            return;
        }

        CreateNodeMenu menu = Arrays.stream(CreateNodeMenu.values())
                .filter(m -> m.getTargetDtoClass() == parentDir.getClass())
                .findFirst()
                .orElse(CreateNodeMenu.TEST_PROJECT);

        new CreateNodesDialog(menu, null, (enteredName, selectedType) -> {
            if (enteredName == null || enteredName.isEmpty()) return;

            Path newDirPath = parentDir.getPath().resolve(enteredName);

            if (selectedType != null && selectedType.getCreator() != null) {
                selectedType.getCreator().execute(
                        this,
                        e.getProject(),
                        enteredName,
                        parentNode,
                        parentDir,
                        newDirPath
                );
            } else {
                System.out.println("No creation logic defined for type: " + selectedType);
            }
        }).show();
    }

    public void createTestRun(final String name, final DirectoryDto parentDir, final Path newDirPath) {
        TestRunDto metadata = new TestRunDto();
        metadata.setStatus(TestRunStatus.CREATED);

        TestRunDirectoryDto tr = new TestRunDirectoryDto()
                .setName(name)
                .setPath(newDirPath);

        TreeUtilImpl.createVf(this, parentDir.getPath(), name);
        TreeUtilImpl.createDataVf(this, newDirPath, DirectoryType.TR.getMarker());

        RunEditor.create(
                tr,
                projectPanel,
                projectPanel.getTestProjectSelector().getSelectedTestProject().getItem(),
                metadata
        );
    }

    public void createTestSet(final Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {
        TestSetDirectoryDto newTestSetDirectory = new TestSetDirectoryDto()
                .setName(name)
                .setPath(parentDir.getPath().resolve(name));

        TreeUtilImpl.createVf(this, parentDir.getPath(), newTestSetDirectory.getName());
        TreeUtilImpl.createDataVf(this, newDirPath, DirectoryType.TS.getMarker());
        TreeUtilImpl.createNode(tree, parentNode, newTestSetDirectory);

        Tools.createJavaClassInTestRoot(project, parentDir.getName(), name);
        TestEditor.open(newTestSetDirectory);
    }

    public void createTestSetPackage(final Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {
        TestSetPackageDirectoryDto newTestSetPackageDirectory = new TestSetPackageDirectoryDto()
                .setName(name)
                .setPath(parentDir.getPath().resolve(name));

        TreeUtilImpl.createVf(this, parentDir.getPath(), name);
        TreeUtilImpl.createNode(tree, parentNode, newTestSetPackageDirectory);
        TreeUtilImpl.createDataVf(this, newDirPath, DirectoryType.TSP.getMarker());

        Tools.createJavaPackageInTestRoot(project, name);
    }

    public void createTestRunPackage(final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {
        TestRunPackageDirectoryDto newTestRunPackageDirectory = new TestRunPackageDirectoryDto()
                .setName(name)
                .setPath(parentDir.getPath().resolve(name));

        TreeUtilImpl.createVf(this, parentDir.getPath(), name);
        TreeUtilImpl.createNode(tree, parentNode, newTestRunPackageDirectory);
        TreeUtilImpl.createDataVf(this, newDirPath, DirectoryType.TRP.getMarker());
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        if (path == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        if (userObject == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        final CreateNodeMenu menu = Arrays.stream(CreateNodeMenu.values())
                .filter(m -> m.getTargetDtoClass() == userObject.getClass())
                .findFirst()
                .orElse(null);

        boolean hasOptions = menu != null && !menu.getAvailableOptions().isEmpty();
        e.getPresentation().setVisible(true);
        e.getPresentation().setEnabled(hasOptions);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}