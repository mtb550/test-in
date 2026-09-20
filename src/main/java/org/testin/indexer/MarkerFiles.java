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

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.DirectoryType;
import org.testin.model.markers.AbstractMarker;
import org.testin.model.markers.Marker;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.util.Mapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
final class MarkerFiles {
    private final @NotNull Project p;

    private final @NotNull Set<String> damaged = ConcurrentHashMap.newKeySet();

    // UC-INTERNAL-002, Rule-INTERNAL-014
    @SuppressWarnings("unchecked")
    <M extends AbstractMarker> @NotNull M read(final @NotNull Path dirPath, final @NotNull DirectoryType kind, final @NotNull String name) {
        final @NotNull Class<M> markerClass = (Class<M>) kind.getMarkerClass();
        final @NotNull Path markerFile = dirPath.resolve(kind.getMarker());

        if (!Files.exists(markerFile)) return defaultFor(markerClass, kind);

        try {
            return Services.getInstance(p, Mapper.class).readValue(markerFile.toFile(), markerClass);

        } catch (final Exception ex) {
            Logger.warn("Unreadable " + kind.getMarkerKind() + " marker '" + name + "', using defaults: " + ex.getMessage());

            damaged.add(name);
            return defaultFor(markerClass, kind);
        }
    }

    boolean write(final @NotNull Path dirPath, final @NotNull String markerFileName, final @NotNull Object marker) {
        final @NotNull Path file = dirPath.resolve(markerFileName);

        // UC-INTERNAL-002, Rule-INTERNAL-083
        if (marker instanceof Marker m && m.getCreatedBy().isEmpty()) {
            if (!Files.exists(file)) {
                m.stampCreated(tester());
            } else if (!parses(file, marker.getClass())) {
                Logger.warn("Left the unreadable marker " + file + " as it is rather than writing defaults over it");
                return false;
            }
        }

        // Rule-INTERNAL-090
        if (marker instanceof Marker m && m.getId().isEmpty()) m.setId(UUID.randomUUID().toString());

        return Services.getInstance(p, TestDataFiles.class).write(p, file, marker);
    }

    // Rule-INTERNAL-090, Rule-TREE-PANEL-051
    boolean giveFreshId(final @NotNull Path markerFile) {
        final @NotNull Optional<DirectoryType> kind = DirectoryType.byMarker(String.valueOf(markerFile.getFileName()));
        if (kind.isEmpty()) return false;

        try {
            final @NotNull AbstractMarker marker = Services.getInstance(p, Mapper.class).readValue(markerFile.toFile(), kind.orElseThrow().getMarkerClass());
            marker.setId(UUID.randomUUID().toString());

            return Services.getInstance(p, TestDataFiles.class).write(p, markerFile, marker);

        } catch (final Exception ex) {
            Logger.warn("Left the copied marker " + markerFile + " without an id of its own: " + ex.getMessage());
            return false;
        }
    }

    private boolean parses(final @NotNull Path file, final @NotNull Class<?> markerClass) {
        try {
            Services.getInstance(p, Mapper.class).readValue(file.toFile(), markerClass);
            return true;
        } catch (final Exception unreadable) {
            return false;
        }
    }

    void touched(final @NotNull Path dirPath, final @NotNull String markerFileName, final @NotNull Marker marker) {
        marker.touch(tester());

        write(dirPath, markerFileName, marker);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-014
    @NotNull List<String> takeDamaged() {
        final @NotNull List<String> taken = List.copyOf(damaged);
        damaged.clear();

        return taken;
    }

    boolean has(final @NotNull Path dirPath, final @NotNull DirectoryType kind) {
        return Files.exists(dirPath.resolve(kind.getMarker()));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-009
    @NotNull Optional<DirectoryType> markedAs(final @NotNull Path dirPath, final @NotNull List<DirectoryType> family) {
        return family.stream().filter(kind -> has(dirPath, kind)).findFirst();
    }

    private @NotNull String tester() {
        return Services.getInstance(p, AppSettingsState.class).testerName;
    }

    private <M> @NotNull M defaultFor(final @NotNull Class<M> markerClass, final @NotNull DirectoryType kind) {
        try {
            return markerClass.getDeclaredConstructor().newInstance();
        } catch (final Exception ex) {
            throw new RuntimeException("Cannot create default " + kind.getMarkerKind() + " marker", ex);
        }
    }
}
