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
import org.testin.model.markers.Marker;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.util.Mapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The marker file round trip: reading one, writing one, and answering what a
 * directory is marked as.
 * <p>
 * One class owns both halves. The read used to live in {@code DirectoryMapper}
 * and the write in the store, so the indexer owned one end of a file and a
 * mapper owned the other - the debt #49 records, which grew from two markers to
 * seven when #68 fixed the five that were written and never read.
 * <p>
 * <b>A marker that will not parse is not a missing node.</b> The file is a type
 * discriminator as well as a payload, so its directory is a real node either
 * way; dropping it out of the tree would hide test cases over an unreadable
 * audit stamp. The default is used, the name is remembered, and whoever is
 * scanning reports all of them at once.
 * <p>
 * Package-private, behind {@link ProjectIndexer} like everything else that
 * touches a test data file.
 */
@AllArgsConstructor
final class MarkerFiles {

    private final @NotNull Project p;

    /**
     * The nodes whose marker would not parse, so the tester is told once for the
     * project rather than once per node.
     */
    private final @NotNull Set<String> damaged = ConcurrentHashMap.newKeySet();

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-014.
     * <p>
     * A missing or unreadable marker falls back to a default instance rather
     * than failing - see the class note for why.
     */
    <M> @NotNull M read(final @NotNull Path dirPath, final @NotNull DirectoryType kind, final @NotNull Class<M> markerClass, final @NotNull String name) {
        final @NotNull Path markerFile = dirPath.resolve(kind.getMarker());

        // Asked before reading, because a marker that is not there yet is the
        // ordinary case: a node is created, its directory appears, and the marker
        // follows. Handing an absent file to the mapper made it log an ERROR on
        // the way out - one per node created, 135 in a single sandbox session -
        // and those were the first thing a search for ERROR found. Now an ERROR
        // from the mapper means what it says: a file that is there and will not
        // parse (#66).
        if (!Files.exists(markerFile)) return defaultFor(markerClass, kind);

        try {
            return Services.getInstance(p, Mapper.class).readValue(markerFile.toFile(), markerClass);

        } catch (final Exception ex) {
            Logger.warn("Unreadable " + kind.getMarkerKind() + " marker '" + name + "', using defaults: " + ex.getMessage());

            // Remembered as well as logged. The node is still drawn, and drawn
            // looking ordinary - its number, its status and who made it are the
            // defaults rather than what the file says - and the log is at a level
            // most testers never turn on (#277). Whoever is scanning reports it.
            damaged.add(name);
            return defaultFor(markerClass, kind);
        }
    }

    /**
     * Writes the marker, stamping it first when it has never been written: a new
     * marker (createdBy still blank) takes the full audit stamp, and one loaded
     * from disk passes through untouched.
     * <p>
     * The two steps are one call because every caller made both, in this order,
     * and a write that forgot the stamp is a node with no author.
     *
     * @return whether the marker landed. Writing it is what brings the node's
     * directory into existence, so a creation that indexed first and wrote
     * afterwards drew a test set in the tree with nothing on disk (#66,
     * finding 85).
     */
    boolean write(final @NotNull Path dirPath, final @NotNull String markerFileName, final @NotNull Object marker) {
        if (marker instanceof Marker m && m.getCreatedBy().isEmpty()) {
            m.stampCreated(tester());
        }

        return Services.getInstance(p, TestDataFiles.class).write(p, dirPath.resolve(markerFileName), marker);
    }

    /**
     * The marker of a node that has just changed: whoever is at the keyboard
     * becomes its modifier, and it is written.
     */
    void touched(final @NotNull Path dirPath, final @NotNull String markerFileName, final @NotNull Marker marker) {
        marker.touch(tester());

        write(dirPath, markerFileName, marker);
    }

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-014.
     * <p>
     * The nodes whose marker was there and would not parse, since the last time
     * anyone asked, and forgotten in the asking.
     * <p>
     * Handed to the scan to report because a notification per node would be one
     * per node - a project whose markers were all damaged by one bad merge would
     * raise dozens. The scan already reports the folders it could not read this
     * way.
     */
    @NotNull List<String> takeDamaged() {
        final @NotNull List<String> taken = List.copyOf(damaged);
        damaged.clear();

        return taken;
    }

    /**
     * Whether a directory carries one kind's marker.
     */
    boolean has(final @NotNull Path dirPath, final @NotNull DirectoryType kind) {
        return Files.exists(dirPath.resolve(kind.getMarker()));
    }

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-009.
     * <p>
     * What kind a directory is marked as, asked once.
     * <p>
     * The probe lives here rather than on the enum because reading the disk is
     * the indexer's alone (CLAUDE.md), and the order lives on the enum because
     * the precedence is a fact about the kinds rather than about this scan - the
     * split #173 asked for, so {@code model} stays a leaf (#111).
     */
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
