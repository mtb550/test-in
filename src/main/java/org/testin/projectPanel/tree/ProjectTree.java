package org.testin.projectPanel.tree;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProjectTree implements Disposable {
    private final ProjectPanel pp;
    private final JBScrollPane scrollPane;
    private final ProjectTreeStructure treeStructure;
    private final StructureTreeModel<ProjectTreeStructure> structureModel;
    private final AsyncTreeModel treeModel;
    @Getter
    private final SimpleTree mainTree;
    private final TreeTransferHandler transferHandler;
    private final TreeContextMenu treeContextMenu;
    private final AtomicBoolean refreshScheduled = new AtomicBoolean();

    /**
     * Path of the project currently shown in the tree. The tree auto-expands when it
     * loads a different project (startup and selector changes); refreshes of the same
     * project keep the user's own expand/collapse state.
     */
    private volatile String expandedProjectPath;
    private volatile boolean disposed;

    public ProjectTree(final @NotNull Project p, final @NotNull ProjectPanel pp) {
        this.pp = pp;

        final TestProjectDirectoryDto selectedProject = (TestProjectDirectoryDto)
                pp.getTestProjectSelector().getSelectedTestProject().getSelectedItem();
        this.treeStructure = new ProjectTreeStructure(p, selectedProject);
        this.structureModel = new StructureTreeModel<>(treeStructure, this);
        this.treeModel = new AsyncTreeModel(structureModel, this);
        this.mainTree = new SimpleTree(treeModel);
        this.scrollPane = new JBScrollPane(mainTree);

        mainTree.setRootVisible(true);
        mainTree.setShowsRootHandles(true);
        // Nodes can only be moved into a directory.  INSERT also exposes
        // sibling positions, which the transfer handler cannot resolve to a
        // destination directory.
        mainTree.setDropMode(DropMode.ON);
        mainTree.setAutoscrolls(true);

        final Set<DirectoryDto> sharedCutNodes = new HashSet<>();
        mainTree.setCellRenderer(new TreeCellRenderer(sharedCutNodes));

        this.transferHandler = new TreeTransferHandler(p, mainTree, sharedCutNodes, this::refresh);
        mainTree.setTransferHandler(transferHandler);
        mainTree.setDragEnabled(true);

        treeContextMenu = new TreeContextMenu(p, pp, mainTree);
        mainTree.addMouseListener(new TreeMouseListener(p, mainTree, treeContextMenu));
        treeContextMenu.registerShortcuts(mainTree, transferHandler);
    }

    /**
     * Reloads the model from the current indexer state without mutating Swing tree nodes.
     */
    public void refresh() {
        if (disposed || !refreshScheduled.compareAndSet(false, true)) return;

        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                if (disposed) return;
                final TestProjectDirectoryDto selectedProject = (TestProjectDirectoryDto)
                        pp.getTestProjectSelector().getSelectedTestProject().getSelectedItem();
                treeStructure.setSelectedProject(selectedProject);

                final String projectPath = selectedProject != null ? selectedProject.getPath().toString() : null;
                final boolean projectChanged = projectPath != null && !projectPath.equals(expandedProjectPath);
                expandedProjectPath = projectPath;

                structureModel.invalidateAsync().thenRun(() -> {
                    if (!disposed && projectChanged) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                TreeUtil.promiseExpandAll(mainTree));
                    }
                });

                mainTree.revalidate();
                mainTree.repaint();
            } finally {
                refreshScheduled.set(false);
            }
        });
    }

    public void updateNodes() {
        pp.getTestProjectSelector().loadTestProjectList();
    }

    public JComponent getComponent() {
        return scrollPane;
    }

    @Override
    public void dispose() {
        disposed = true;
        mainTree.setModel(null);
        treeModel.dispose();
        structureModel.dispose();
    }
}
