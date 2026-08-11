package org.testin.projectPanel.tree;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.services.Services;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Transfers application values and lets the async model rebuild from indexer state.
 */
public class TreeTransferHandler extends TransferHandler {
    public static final DataFlavor NODE_FLAVOR =
            new DataFlavor(TreeTransferPayload.class, "Testin tree nodes");

    private final @NotNull Project p;
    private final SimpleTree tree;
    private final Runnable refresh;
    @Getter
    private final Set<DirectoryDto> selectedNodes;
    private int clipboardAction = COPY;

    public TreeTransferHandler(final @NotNull Project p, final SimpleTree tree,
                               final Set<DirectoryDto> selectedNodes, final Runnable refresh) {
        this.p = p;
        this.tree = tree;
        this.selectedNodes = selectedNodes;
        this.refresh = refresh;
    }

    @Override
    public int getSourceActions(final JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    protected Transferable createTransferable(final JComponent c) {
        final List<DirectoryDto> directories = TreeValueUtil.selectedDirectories(tree.getSelectionPaths());
        return directories.isEmpty() ? null : new NodesTransferable(new TreeTransferPayload(
                directories.toArray(DirectoryDto[]::new), clipboardAction));
    }

    @Override
    public boolean canImport(final TransferSupport support) {
        if (!support.isDataFlavorSupported(NODE_FLAVOR)) return false;
        final DirectoryDto target = targetDirectory(support);
        if (target == null) return false;

        if (support.isDrop() && support.getDropAction() == NONE) {
            support.setDropAction(MOVE);
        }
        return true;
    }

    @Override
    public boolean importData(final TransferSupport support) {
        if (!canImport(support)) return false;

        try {
            final TreeTransferPayload payload = (TreeTransferPayload) support.getTransferable().getTransferData(NODE_FLAVOR);
            final DirectoryDto[] sources = payload.nodes();
            final DirectoryDto target = targetDirectory(support);
            if (target == null) return false;

            final int action = resolveAction(support, payload);
            if (action == MOVE) {
                final List<DirectoryDto> movableSources = new ArrayList<>();
                for (DirectoryDto source : sources) {
                    if (isValidMove(source, target)) movableSources.add(source);
                }

                if (movableSources.isEmpty()) return false;
                moveNodes(movableSources, target);
            } else if (action == COPY) {
                final List<Path> sourcePaths = java.util.Arrays.stream(sources).map(DirectoryDto::getPath).toList();
                Services.getInstance(p, ProjectIndexer.class).copyNodes(sourcePaths, target.getPath(), refresh);
            } else {
                return false;
            }

            resetLastAction();
            return true;
        } catch (final Exception ex) {
            Logger.error("Tree transfer failed: " + ex.getMessage());
            return false;
        }
    }

    private DirectoryDto targetDirectory(final TransferSupport support) {
        if (support.isDrop()) {
            final TreePath path = dropPath(support);
            return path == null ? null : TreeValueUtil.directoryOf(path.getLastPathComponent());
        }
        return TreeValueUtil.selectedDirectory(tree.getSelectionPath());
    }

    private TreePath dropPath(final TransferSupport support) {
        if (support.getDropLocation() instanceof SimpleTree.DropLocation dropLocation) {
            return dropLocation.getPath();
        }
        if (support.getDropLocation() instanceof JTree.DropLocation dropLocation) {
            return dropLocation.getPath();
        }
        return null;
    }

    private int resolveAction(final TransferSupport support, final TreeTransferPayload payload) {
        if (!support.isDrop()) {
            return payload.clipboardAction() == MOVE ? MOVE : COPY;
        }

        // Keep Ctrl-drag copy support. A normal internal tree drag is a move,
        // including platforms that report NONE before the drop action is set.
        if (support.getUserDropAction() == COPY) return COPY;
        support.setDropAction(MOVE);
        return MOVE;
    }

    private boolean isValidMove(final DirectoryDto source, final DirectoryDto target) {
        final Path sourcePath = source.getPath().normalize();
        final Path targetPath = target.getPath().normalize();

        if (sourcePath.equals(targetPath) || targetPath.startsWith(sourcePath)) return false;
        return !targetPath.equals(sourcePath.getParent());
    }

    private void moveNodes(final List<DirectoryDto> sources, final DirectoryDto target) {
        final AtomicInteger remaining = new AtomicInteger(sources.size());
        for (DirectoryDto source : sources) {
            persistMove(source, target, () -> {
                if (remaining.decrementAndGet() == 0) refresh.run();
            });
        }
    }

    private void persistMove(final DirectoryDto source, final DirectoryDto target, final Runnable onFinished) {
        final Path newPath = target.getPath().resolve(source.getName());
        Services.getInstance(p, ProjectIndexer.class).moveNode(source.getPath(), newPath, onFinished);
    }

    @Override
    protected void exportDone(final JComponent source, final Transferable data, final int action) {
        if (action != MOVE) resetLastAction();
    }

    public void resetLastAction() {
        selectedNodes.clear();
        tree.repaint();
    }

    @Override
    public void exportToClipboard(final JComponent comp, final Clipboard clip, final int action) {
        clipboardAction = action;
        try {
            super.exportToClipboard(comp, clip, action);
        } finally {
            clipboardAction = COPY;
        }
        updateClipboardState(action, TreeValueUtil.selectedDirectories(tree.getSelectionPaths()));
    }

    public boolean copySelectionToClipboard(final boolean cut) {
        final List<DirectoryDto> directories = TreeValueUtil.selectedDirectories(tree.getSelectionPaths());
        if (directories.isEmpty()) return false;

        final int action = cut ? MOVE : COPY;
        CopyPasteManager.getInstance().setContents(new NodesTransferable(new TreeTransferPayload(
                directories.toArray(DirectoryDto[]::new), action)));
        updateClipboardState(action, directories);
        return true;
    }

    public boolean pasteFromClipboard() {
        final Transferable contents = CopyPasteManager.getInstance().getContents();
        return contents != null && importData(new TransferSupport(tree, contents));
    }

    private void updateClipboardState(final int action, final List<DirectoryDto> directories) {
        selectedNodes.clear();
        if (action == MOVE) selectedNodes.addAll(directories);
        tree.repaint();
    }

}
