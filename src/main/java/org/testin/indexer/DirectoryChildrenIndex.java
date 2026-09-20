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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

final class DirectoryChildrenIndex {
    private volatile @NotNull Map<Path, List<DirectoryDto>> childrenByParent = Map.of();

    private final @NotNull AtomicLong invalidations = new AtomicLong();
    private volatile long builtAfter = -1;

    @NotNull
    List<DirectoryDto> get(final @NotNull Path parentPath, final @NotNull Supplier<Collection<DirectoryDto>> source) {
        rebuildIfNeeded(source);
        return childrenByParent.getOrDefault(parentPath, List.of());
    }

    void invalidate() {
        invalidations.incrementAndGet();
    }

    void clear() {
        childrenByParent = Map.of();
        invalidate();
    }

    private static final @NotNull Comparator<DirectoryDto> BY_ARRANGEMENT = Comparator
            .comparing(DirectoryDto::isRetired)
            .thenComparingInt(DirectoryDto::getOrder)
            .thenComparing(node -> node.getMarker().getCreatedAt())
            .thenComparing(DirectoryDto::getName);

    private void rebuildIfNeeded(final @NotNull Supplier<Collection<DirectoryDto>> source) {
        if (builtAfter == invalidations.get()) return;
        synchronized (this) {
            final long seen = invalidations.get();
            if (builtAfter == seen) return;

            final @NotNull Map<Path, List<DirectoryDto>> rebuilt = new HashMap<>();
            for (final DirectoryDto directory : source.get()) {
                Optional.ofNullable(directory.getParent()).ifPresent(parent ->
                        rebuilt.computeIfAbsent(parent.getPath(), ignored -> new ArrayList<>()).add(directory));
            }
            rebuilt.values().forEach(children -> children.sort(BY_ARRANGEMENT));
            rebuilt.replaceAll((parent, children) -> List.copyOf(children));

            childrenByParent = Map.copyOf(rebuilt);
            builtAfter = seen;
        }
    }
}
