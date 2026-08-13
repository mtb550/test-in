package org.testin.enums;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.dirs.DirectoryDto;

@FunctionalInterface
public interface RemoveHandler {

    /**
     * Removes the node, then runs {@code onRemoved}. The callback is what makes
     * the ordering safe: the VFS delete is asynchronous, and the indexer cache
     * must only be updated once it has happened (see CLAUDE.md - the other order
     * leaves phantom directories behind).
     */
    void remove(final @NotNull Project p, final @NotNull DirectoryDto dir, final @NotNull Runnable onRemoved);
}
