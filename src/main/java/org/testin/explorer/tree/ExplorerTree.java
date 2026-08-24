package org.testin.explorer.tree;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.TreeVisitor;
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
import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExplorerTree implements Disposable {

    /**
     * What a reveal does afterward when the caller wants nothing - the same
     * shape the view panel uses for the same reason.
     */
    private static final @NotNull Runnable NOTHING_AFTER = () -> {
    };

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
     * Expands to a node and selects it, wherever it is (#29).
     * <p>
     * Matched on the path the node already carries rather than on a node object,
     * because the tree builds its own wrappers as it expands and the caller has
     * the one the indexer holds - two objects for one node, and only one of them
     * is ever in the tree.
     * <p>
     * The visitor is what makes this work at any depth: the platform walks from
     * the root, and a branch whose path is not a prefix of the target is not
     * expanded at all, so revealing a case eight levels down opens eight nodes
     * rather than the whole tree.
     */
    public void reveal(final @NotNull Path target) {
        reveal(target, NOTHING_AFTER);
    }

    /**
     * The same, and then whatever the caller wanted done once the node is
     * actually there.
     * <p>
     * A callback rather than a returned promise, because the one thing anybody
     * wants afterward is the focus - and asking for it before the walk finishes
     * puts it on a row the tree has not selected yet.
     *
     * @param afterFound run on the EDT once the node is selected and scrolled to
     */
    public void reveal(final @NotNull Path target, final @NotNull Runnable afterFound) {
        if (disposed) return;

        TreeUtil.promiseSelect(mainTree, new TreeVisitor() {
            @Override
            public @NotNull Action visit(final @NotNull TreePath path) {
                final @NotNull Optional<Path> at = TreeValueUtil.directoryAt(path).map(DirectoryDto::getPath);
                if (at.isEmpty()) return Action.CONTINUE;

                if (at.get().equals(target)) return Action.INTERRUPT;

                return target.startsWith(at.get()) ? Action.CONTINUE : Action.SKIP_CHILDREN;
            }
        }).onSuccess(found -> ApplicationManager.getApplication().invokeLater(() -> {
            if (disposed) return;

            mainTree.scrollPathToVisible(found);
            afterFound.run();
        }));
    }

    /**
     * Puts the keyboard on the tree, for a tester who asked to be taken to a
     * node: the node is selected, and the arrow keys should move from it.
     */
    public void focus() {
        mainTree.requestFocusInWindow();
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
