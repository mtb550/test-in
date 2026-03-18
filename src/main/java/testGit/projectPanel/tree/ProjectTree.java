package testGit.projectPanel.tree;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.Getter;
import testGit.pojo.dto.dirs.TestProjectDirectoryDto;
import testGit.projectPanel.ProjectPanel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.HashSet;
import java.util.Set;

@Getter
public class ProjectTree {
    private final ProjectPanel projectPanel;
    private final JBScrollPane scrollPane;
    private final DefaultMutableTreeNode mainRoot;
    private final DefaultTreeModel treeModel;
    private final SimpleTree mainTree;
    private final TreeTransferHandler transferHandler;
    private final TreeContextMenu treeContextMenu;

    public ProjectTree(ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;

        TestProjectDirectoryDto testProjectDirectory = null;
        if (projectPanel.getTestProjectSelector() != null && projectPanel.getTestProjectSelector().getSelectedTestProject() != null) {
            testProjectDirectory = (TestProjectDirectoryDto) projectPanel.getTestProjectSelector().getSelectedTestProject().getSelectedItem();
        }

        this.mainRoot = new DefaultMutableTreeNode(testProjectDirectory != null ? testProjectDirectory : "Project");
        this.treeModel = new DefaultTreeModel(mainRoot);
        this.mainTree = new SimpleTree(treeModel);
        this.scrollPane = new JBScrollPane(mainTree);

        mainTree.setRootVisible(false);
        mainTree.setShowsRootHandles(true);
        mainTree.setDragEnabled(true);
        mainTree.setDropMode(DropMode.ON_OR_INSERT);

        Set<DefaultMutableTreeNode> sharedCutNodes = new HashSet<>();

        updateNodes();

        mainTree.setCellRenderer(new TreeCellRenderer(sharedCutNodes));

        this.transferHandler = new TreeTransferHandler(mainTree, sharedCutNodes);
        mainTree.setTransferHandler(transferHandler);

        treeContextMenu = new TreeContextMenu(projectPanel, mainTree);
        mainTree.addMouseListener(new TreeMouseListener(projectPanel, mainTree, treeContextMenu));

        TreeContextMenu.registerShortcuts(mainTree, transferHandler, treeContextMenu);
    }

    public void updateNodes() {
        ApplicationManager.getApplication().invokeLater(() -> {
            mainRoot.removeAllChildren();

            TestProjectDirectoryDto testProjectDirectory = null;
            if (projectPanel.getTestProjectSelector() != null && projectPanel.getTestProjectSelector().getSelectedTestProject() != null) {
                testProjectDirectory = (TestProjectDirectoryDto) projectPanel.getTestProjectSelector().getSelectedTestProject().getSelectedItem();
            }
            if (testProjectDirectory != null) {
                mainRoot.setUserObject(testProjectDirectory);
            }

            DefaultMutableTreeNode tcNode = projectPanel.getTestCaseTreeBuilder().getRootNode();
            DefaultMutableTreeNode trNode = projectPanel.getTestRunTreeBuilder().getRootNode();

            if (tcNode != null) mainRoot.add(tcNode);
            if (trNode != null) mainRoot.add(trNode);

            treeModel.reload();
            TreeUtil.expandAll(mainTree);
        });
    }

    public JComponent getComponent() {
        return scrollPane;
    }
}