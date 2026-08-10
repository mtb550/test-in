package org.testin.projectPanel.tree;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
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
    private Integer lastAction;

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
        return directories.isEmpty() ? null : new NodesTransferable(new TreeTransferPayload(directories.toArray(DirectoryDto[]::new)));
    }

    @Override
    public boolean canImport(final TransferSupport support) {
        if (!support.isDataFlavorSupported(NODE_FLAVOR)) return false;
        final DirectoryDto target = targetDirectory(support);
        return target != null;
    }

    @Override
    public boolean importData(final TransferSupport support) {
        if (!canImport(support)) return false;

        try {
            final TreeTransferPayload payload = (TreeTransferPayload) support.getTransferable().getTransferData(NODE_FLAVOR);
            final DirectoryDto[] sources = payload.nodes();
            final DirectoryDto target = targetDirectory(support);
            if (target == null) return false;

            final int action = support.isDrop() ? support.getDropAction() : (lastAction != null ? lastAction : COPY);
            for (DirectoryDto source : sources) {
                if (action == MOVE) {
                    if (source.getPath().equals(target.getPath()) || target.getPath().startsWith(source.getPath()))
                        continue;
                    persistMove(source, target);
                }
            }

            if (action == COPY) {
                final List<Path> sourcePaths = java.util.Arrays.stream(sources).map(DirectoryDto::getPath).toList();
                Services.getInstance(p, ProjectIndexer.class).copyNodes(sourcePaths, target.getPath(), refresh);
            }

            resetLastAction();
            if (action == MOVE) refresh.run();
            return true;
        } catch (final Exception ex) {
            Logger.error("Tree transfer failed: " + ex.getMessage());
            return false;
        }
    }

    private DirectoryDto targetDirectory(final TransferSupport support) {
        if (support.isDrop()) {
            final TreePath path = ((SimpleTree.DropLocation) support.getDropLocation()).getPath();
            return path == null ? null : TreeValueUtil.directoryOf(path.getLastPathComponent());
        }
        return TreeValueUtil.selectedDirectory(tree.getSelectionPath());
    }

    private void persistMove(final DirectoryDto source, final DirectoryDto target) {
        final Path newPath = target.getPath().resolve(source.getName());
        Services.getInstance(p, ProjectIndexer.class).moveNode(source.getPath(), newPath);
        Logger.info("Moved successfully to: " + newPath);
    }

    @Override
    protected void exportDone(final JComponent source, final Transferable data, final int action) {
        resetLastAction();
    }

    public void resetLastAction() {
        lastAction = null;
        selectedNodes.clear();
        tree.repaint();
    }

    @Override
    public void exportToClipboard(final JComponent comp, final Clipboard clip, final int action) {
        super.exportToClipboard(comp, clip, action);
        lastAction = action;
        selectedNodes.clear();
        if (action == MOVE) selectedNodes.addAll(TreeValueUtil.selectedDirectories(tree.getSelectionPaths()));
        tree.repaint();
    }

    private record NodesTransferable(TreeTransferPayload payload) implements Transferable {
        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{NODE_FLAVOR, DataFlavor.javaFileListFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(final DataFlavor flavor) {
            return NODE_FLAVOR.equals(flavor) || DataFlavor.javaFileListFlavor.equals(flavor);
        }

        @Override
        public @NotNull Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException {
            if (NODE_FLAVOR.equals(flavor)) return payload;
            if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                final List<File> files = new ArrayList<>();
                for (DirectoryDto node : payload.nodes()) {
                    if (node instanceof TestSetDirectoryDto || node instanceof TestRunDirectoryDto)
                        files.add(node.getPath().toFile());
                }
                return files;
            }
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
