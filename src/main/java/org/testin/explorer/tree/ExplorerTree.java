package org.testin.explorer.tree;

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
import org.testin.explorer.ExplorerPanel;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;

import javax.swing.*;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExplorerTree implements Disposable {
    private final @NotNull Project p;
    private final @NotNull ExplorerPanel pp;
    private final @NotNull JBScrollPane scrollPane;
    private final @NotNull ExplorerTreeStructure treeStructure;
    private final @NotNull StructureTreeModel<ExplorerTreeStructure> structureModel;
    private final @NotNull AsyncTreeModel treeModel;
    @Getter
    private final @NotNull SimpleTree mainTree;
    private final @NotNull AtomicBoolean refreshScheduled = new AtomicBoolean();

    /**
     * Path of the project currently shown in the tree. The tree auto-expands when it
     * loads a different project (startup and selector changes); refreshes of the same
     * project keep the user's own expand/collapse state. Empty while no project is selected.
     */
    private volatile @NotNull String expandedProjectPath = "";
    private volatile boolean disposed;

    public ExplorerTree(final @NotNull Project p, final @NotNull ExplorerPanel pp) {
        this.p = p;
        this.pp = pp;

        this.treeStructure = new ExplorerTreeStructure(p, bound());
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

        final @NotNull Set<DirectoryDto> sharedCutNodes = new HashSet<>();
        mainTree.setCellRenderer(new TreeCellRenderer(sharedCutNodes));

        final @NotNull TreeTransferHandler transferHandler = new TreeTransferHandler(p, mainTree, sharedCutNodes, this::refresh);
        mainTree.setTransferHandler(transferHandler);
        mainTree.setDragEnabled(true);

        final @NotNull TreeContextMenu treeContextMenu = new TreeContextMenu(p, pp, mainTree);
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
                final @NotNull Optional<TestProjectDirectoryDto> boundProject = bound();
                treeStructure.setSelectedProject(boundProject);

                final @NotNull String projectPath = boundProject.map(dir -> dir.getPath().toString()).orElse("");
                final boolean projectChanged = !projectPath.isEmpty() && !projectPath.equals(expandedProjectPath);
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
        pp.refresh();
    }

    /**
     * The test project this repository is bound to, asked for fresh each time.
     * The tree used to read it out of a combo box; it now reads it from the one
     * service that answers the question (#8).
     */
    private @NotNull Optional<TestProjectDirectoryDto> bound() {
        return Services.getInstance(p, BoundTestProject.class).get();
    }

    public @NotNull JComponent getComponent() {
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
