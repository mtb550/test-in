package testGit.projectPanel.tree;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.Getter;
import testGit.pojo.TestProject;
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
    private final TransferHandlerImpl transferHandler;
    private final ContextMenu contextMenu;

    public ProjectTree(ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;

        TestProject testProject = null;
        if (projectPanel.getTestProjectSelector() != null && projectPanel.getTestProjectSelector().getSelectedTestProject() != null) {
            testProject = (TestProject) projectPanel.getTestProjectSelector().getSelectedTestProject().getSelectedItem();
        }

        this.mainRoot = new DefaultMutableTreeNode(testProject != null ? testProject : "Project");
        this.treeModel = new DefaultTreeModel(mainRoot);
        this.mainTree = new SimpleTree(treeModel);
        this.scrollPane = new JBScrollPane(mainTree);

        mainTree.setRootVisible(false);
        mainTree.setShowsRootHandles(true);
        mainTree.setDragEnabled(true);
        mainTree.setDropMode(DropMode.ON_OR_INSERT);

        Set<DefaultMutableTreeNode> sharedCutNodes = new HashSet<>();

        updateNodes();

        mainTree.setCellRenderer(new RendererImpl(sharedCutNodes));
        this.transferHandler = new TransferHandlerImpl(mainTree, sharedCutNodes);
        mainTree.setTransferHandler(transferHandler);
        contextMenu = new ContextMenu(projectPanel, mainTree);
        mainTree.addMouseListener(new MouseAdapterImpl(projectPanel, mainTree, contextMenu));
        ContextMenu.registerShortcuts(mainTree, transferHandler, contextMenu);
    }

    public void updateNodes() {
        ApplicationManager.getApplication().invokeLater(() -> {
            mainRoot.removeAllChildren();

            TestProject testProject = null;
            if (projectPanel.getTestProjectSelector() != null && projectPanel.getTestProjectSelector().getSelectedTestProject() != null) {
                testProject = (TestProject) projectPanel.getTestProjectSelector().getSelectedTestProject().getSelectedItem();
            }
            if (testProject != null) {
                mainRoot.setUserObject(testProject);
            }

            DefaultMutableTreeNode tcNode = projectPanel.getTestCaseTabController().getRootNode();
            DefaultMutableTreeNode trNode = projectPanel.getTestRunTabController().getRootNode();

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