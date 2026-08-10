package org.testin.projectPanel.tree;

import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.dirs.DirectoryDto;

/**
 * Stable local drag/drop payload; avoids JVM array-class DataFlavor resolution issues.
 */
public record TreeTransferPayload(@NotNull DirectoryDto[] nodes) {
}
