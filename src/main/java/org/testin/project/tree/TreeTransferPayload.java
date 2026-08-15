package org.testin.project.tree;

import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.dirs.DirectoryDto;

import javax.swing.*;

/**
 * Stable local drag/drop payload; avoids JVM array-class DataFlavor resolution issues.
 */
public record TreeTransferPayload(@NotNull DirectoryDto[] nodes, int clipboardAction) {
    public TreeTransferPayload(final @NotNull DirectoryDto[] nodes) {
        this(nodes, TransferHandler.COPY);
    }
}
