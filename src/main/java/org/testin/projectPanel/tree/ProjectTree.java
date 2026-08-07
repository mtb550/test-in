package org.testin.projectPanel.tree;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.ProjectStatus;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.HashSet;
import java.util.Set;

@Getter
public class ProjectTree {
    private final @NotNull Project p;
    private final ProjectPanel pp;
    private final JBScrollPane scrollPane;
    private final DefaultMutableTreeNode mainRoot;
    private final DefaultTreeModel treeModel;
    private final SimpleTree mainTree;
    private final TreeTransferHandler transferHandler;
    private final TreeContextMenu treeContextMenu;

    public ProjectTree(final @NotNull Project p, final @NotNull ProjectPanel pp) {
        this.p = p;
        this.pp = pp;

        final TestProjectDirectoryDto testProjectDirectory = (TestProjectDirectoryDto) pp.getTestProjectSelector().getSelectedTestProject().getSelectedItem();

        this.mainRoot = new DefaultMutableTreeNode(testProjectDirectory != null ? testProjectDirectory : "Project");
        this.treeModel = new DefaultTreeModel(mainRoot);
        this.mainTree = new SimpleTree(treeModel);
        this.scrollPane = new JBScrollPane(mainTree);

        mainTree.setRootVisible(true);
        mainTree.setShowsRootHandles(true);
        mainTree.setDragEnabled(true);
        mainTree.setDropMode(DropMode.ON_OR_INSERT);

        Set<DefaultMutableTreeNode> sharedCutNodes = new HashSet<>();

        updateNodes();

        mainTree.setCellRenderer(new TreeCellRenderer(sharedCutNodes));

        this.transferHandler = new TreeTransferHandler(p, mainTree, sharedCutNodes);
        mainTree.setTransferHandler(transferHandler);

        treeContextMenu = new TreeContextMenu(p, pp, mainTree);
        mainTree.addMouseListener(new TreeMouseListener(p, mainTree, treeContextMenu));

        treeContextMenu.registerShortcuts(mainTree, transferHandler);
    }

    public void updateNodes() {
        ApplicationManager.getApplication().invokeLater(() -> {
            pp.getTestProjectSelector().loadTestProjectList();
            doRefreshTree();
        });
    }

    private void doRefreshTree() {
        mainRoot.removeAllChildren();

        final TestProjectDirectoryDto testProjectDirectory = (TestProjectDirectoryDto) pp.getTestProjectSelector().getSelectedTestProject().getSelectedItem();

        if (testProjectDirectory != null) {
            mainRoot.setUserObject(testProjectDirectory);

            if (testProjectDirectory.getMarker().getStatus() == ProjectStatus.ACTIVE) {
                DefaultMutableTreeNode tcNode = pp.getTestCaseTreeBuilder().getRootNode();
                DefaultMutableTreeNode trNode = pp.getTestRunTreeBuilder().getRootNode();

                if (tcNode != null) mainRoot.add(tcNode);
                if (trNode != null) mainRoot.add(trNode);
            }
        }

        treeModel.reload();
        TreeUtil.expandAll(mainTree);
    }

    public void refreshTree() {
        ApplicationManager.getApplication().invokeLater(this::doRefreshTree);
    }

    public JComponent getComponent() {
        return scrollPane;
    }
}