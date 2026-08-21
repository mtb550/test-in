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

    /**
     * How a folder reads: live nodes before retired ones, then the number the
     * tester gave, then the date it was created, then the name.
     * <p>
     * No rule about nodes nobody numbered, because there is nothing to say: a
     * node with no number carries {@link Marker#NOT_ORDERED}, which is the
     * largest number there is and sorts after every real one on its own.
     * <p>
     * Two nodes with the same number is not a problem to fix either. The date
     * decides between them, so a set can be put third without renumbering the
     * set that was third already.
     */
    private static final @NotNull Comparator<DirectoryDto> BY_ARRANGEMENT = Comparator
            .comparing(DirectoryDto::isRetired)
            .thenComparingInt(DirectoryDto::getOrder)
            .thenComparing(node -> node.getMarker().getCreatedAt())
            .thenComparing(DirectoryDto::getName);

    private void rebuildIfNeeded(final @NotNull Supplier<Collection<DirectoryDto>> source) {
        if (!dirty) return;
        synchronized (this) {
            if (!dirty) return;

            final @NotNull Map<Path, List<DirectoryDto>> rebuilt = new ConcurrentHashMap<>();
            for (final DirectoryDto directory : source.get()) {
                // A test project sits under nothing, so it is nobody's child.
                Optional.ofNullable(directory.getParent()).ifPresent(parent ->
                        rebuilt.computeIfAbsent(parent.getPath(), ignored -> new ArrayList<>()).add(directory));
            }
            // Retired nodes - archived packages, deprecated test sets - sort after
            // the live ones, so last quarter's work stops being the first thing in
            // the tree.
            //
            // Within each half: the order a tester arranged, then by name for
            // everything they have not. A folder nobody has dragged in reads
            // exactly as it always did, which is why nothing had to be converted
            // when nodes learned to carry a rank.
            rebuilt.values().forEach(children -> children.sort(BY_ARRANGEMENT));

            childrenByParent.clear();
            rebuilt.forEach((parent, children) -> childrenByParent.put(parent, List.copyOf(children)));
            dirty = false;
        }
    }
}
