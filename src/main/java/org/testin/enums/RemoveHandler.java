package org.testin.enums;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.dirs.DirectoryDto;

import java.util.function.Consumer;

@FunctionalInterface
public interface RemoveHandler {

    /**
     * Removes the node, then reports whether it went. The callback is what makes
     * the ordering safe: the VFS delete is asynchronous, and the indexer cache
     * must only be updated once it has happened (see CLAUDE.md - the other order
     * leaves phantom directories behind).
     * <p>
     * It always runs, so a caller counting completions is never left waiting, and
     * its argument says which kind of completion it was: false for a delete the
     * VFS refused, and for the fixed containers that are never removed at all.
     */
    void remove(final @NotNull Project p, final @NotNull DirectoryDto dir,
                final @NotNull Consumer<@NotNull Boolean> onRemoved);
}
