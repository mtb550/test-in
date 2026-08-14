package org.testin.projectPanel.tree;

import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Transferable for tree drag-and-drop and clipboard: carries the node payload
 * plus a file-list flavor so nodes can be dropped onto external targets.
 */
record NodesTransferable(@NotNull TreeTransferPayload payload) implements Transferable {

    @Override
    public @NotNull DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{TreeTransferHandler.NODE_FLAVOR, DataFlavor.javaFileListFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(final @NotNull DataFlavor flavor) {
        return TreeTransferHandler.NODE_FLAVOR.equals(flavor) || DataFlavor.javaFileListFlavor.equals(flavor);
    }

    /**
     * The {@code throws} stays deliberately, and is one of the two exceptions to
     * the rule in CLAUDE.md that a method handles its own failures. AWT's
     * {@link java.awt.datatransfer.Transferable} contract is that an unsupported
     * flavor throws; catching it here would hand the platform's drag-and-drop a
     * wrong object instead of telling it the flavor is not available (#63).
     */
    @Override
    public @NotNull Object getTransferData(final @NotNull DataFlavor flavor) throws UnsupportedFlavorException {
        if (TreeTransferHandler.NODE_FLAVOR.equals(flavor)) return payload;
        if (DataFlavor.javaFileListFlavor.equals(flavor)) {
            final List<File> files = new ArrayList<>();
            for (final DirectoryDto node : payload.nodes()) {
                if (node instanceof TestSetDirectoryDto || node instanceof TestRunDirectoryDto)
                    files.add(node.getPath().toFile());
            }
            return files;
        }
        throw new UnsupportedFlavorException(flavor);
    }
}
