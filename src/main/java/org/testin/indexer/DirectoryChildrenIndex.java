/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.indexer;

import org.jetbrains.annotations.NotNull;
import org.testin.model.markers.Marker;
import org.testin.model.dto.dirs.DirectoryDto;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

/**
 * Cached parent-to-children lookup used by the asynchronous project tree.
 */
final class DirectoryChildrenIndex {

    /**
     * Replaced whole, never edited in place.
     * <p>
     * A reader that finds the index clean goes straight to the map without
     * taking the lock, which is the point of the flag. The rebuild used to clear
     * the live map and put the entries back one at a time, so such a reader
     * could look between the two and get {@code List.of()} - a node drawn with
     * no children under it, for no reason it could ever repeat. The rebuild
     * already built its answer separately, and now it swaps that in as one
     * assignment: a reader sees the old map or the new one (#66, finding 84).
     */
    private volatile @NotNull Map<Path, List<DirectoryDto>> childrenByParent = Map.of();
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
        childrenByParent = Map.of();
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

            final @NotNull Map<Path, List<DirectoryDto>> rebuilt = new HashMap<>();
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
            rebuilt.replaceAll((parent, children) -> List.copyOf(children));

            childrenByParent = Map.copyOf(rebuilt);
            dirty = false;
        }
    }
}
