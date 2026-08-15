package org.testin.indexer;

import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.dirs.DirectoryDto;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Cached parent-to-children lookup used by the asynchronous project tree.
 */
final class DirectoryChildrenIndex {
    private final @NotNull Map<Path, List<DirectoryDto>> childrenByParent = new ConcurrentHashMap<>();
    private volatile boolean dirty = true;

    @NotNull
    List<DirectoryDto> get(final @NotNull Path parentPath, final @NotNull Supplier<Collection<DirectoryDto>> source) {
        rebuildIfNeeded(source);
        return childrenByParent.getOrDefault(parentPath, List.of());
    }

    void invalidate() {
        dirty = true;
    }

    void clear() {
        childrenByParent.clear();
        dirty = true;
    }

    private void rebuildIfNeeded(final @NotNull Supplier<Collection<DirectoryDto>> source) {
        if (!dirty) return;
        synchronized (this) {
            if (!dirty) return;

            final Map<Path, List<DirectoryDto>> rebuilt = new ConcurrentHashMap<>();
            for (final DirectoryDto directory : source.get()) {
                if (directory.getParent() == null) continue;
                rebuilt.computeIfAbsent(directory.getParent().getPath(), ignored -> new ArrayList<>()).add(directory);
            }
            rebuilt.values().forEach(children -> children.sort(Comparator.comparing(DirectoryDto::getName)));

            childrenByParent.clear();
            rebuilt.forEach((parent, children) -> childrenByParent.put(parent, List.copyOf(children)));
            dirty = false;
        }
    }
}
