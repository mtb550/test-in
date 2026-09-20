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
import org.testin.model.FileKind;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.markers.AbstractMarker;
import org.testin.model.markers.TestProjectMarker;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

// UC-INTERNAL-008, Rule-INTERNAL-091
@AllArgsConstructor
final class FormatConverter {
    private final @NotNull Project p;

    record Report(@NotNull String project, int cases, int runs, int ids, @NotNull List<String> toRepair, boolean failed) {
        boolean changedAnything() {
            return cases > 0 || runs > 0 || ids > 0;
        }
    }

    // UC-INTERNAL-008, Rule-INTERNAL-091
    @NotNull Optional<Report> convert(final @NotNull Path project) {
        final @NotNull Path markerFile = project.resolve(DirectoryType.TP.getMarker());
        final @NotNull String name = String.valueOf(project.getFileName());

        if (!Files.exists(markerFile)) return Optional.empty();

        final @NotNull Optional<TestProjectMarker> marker = readProjectMarker(markerFile);
        if (marker.isEmpty()) {
            Logger.warn("Not converting " + name + ": its " + DirectoryType.TP.getMarker() + " could not be read");
            return Optional.of(new Report(name, 0, 0, 0, List.of(DirectoryType.TP.getMarker()), true));
        }

        if (marker.orElseThrow().getFormat() >= TestProjectMarker.FORMAT) return Optional.empty();

        Logger.info("Converting " + name + " to format " + TestProjectMarker.FORMAT);

        final @NotNull List<String> toRepair = new ArrayList<>();
        final int cases = toTestCaseFiles(project, toRepair);
        final int runs = removeOldRuns(project);
        final int ids = stampIds(project, toRepair);

        final boolean failed = cases < 0 || runs < 0 || ids < 0;
        if (!failed) stampFormat(markerFile);

        return Optional.of(new Report(name, Math.max(cases, 0), Math.max(runs, 0), Math.max(ids, 0), List.copyOf(toRepair), failed));
    }

    // Rule-INTERNAL-011, Rule-INTERNAL-084
    private int toTestCaseFiles(final @NotNull Path project, final @NotNull List<String> toRepair) {
        final @NotNull Path cases = project.resolve(DirectoryType.TCD.getFolderName());
        if (!Files.isDirectory(cases)) return 0;

        final @NotNull Set<UUID> taken = new HashSet<>();
        int converted = 0;

        for (final Path file : oldCaseFiles(cases)) {
            final @NotNull Optional<UUID> id = identityOf(file);
            if (id.isEmpty()) {
                toRepair.add(String.valueOf(file.getFileName()));
                if (!move(file, file.resolveSibling(baseName(file) + FileKind.TEST_CASE.getExtension()))) return -1;

                converted++;
                continue;
            }

            // Rule-INTERNAL-082
            final @NotNull UUID own = taken.add(id.orElseThrow())
                    ? id.orElseThrow()
                    : derived(id.orElseThrow() + "|" + placeOf(project, file));

            if (!own.equals(id.orElseThrow()) && !reidentify(file, own)) return -1;
            if (!move(file, file.resolveSibling(FileKind.TEST_CASE.fileName(own)))) return -1;

            taken.add(own);
            converted++;
        }

        return converted;
    }

    private int removeOldRuns(final @NotNull Path project) {
        final @NotNull Path runs = project.resolve(DirectoryType.TRD.getFolderName());
        if (!Files.isDirectory(runs)) return 0;

        int removed = 0;
        for (final Path folder : oldRunFolders(runs)) {
            Logger.info("Removing the old-format run " + folder.getFileName() + ": its results cannot be read by this build");

            if (!Services.getInstance(p, TestDataFiles.class).removeTree(p, folder)) return -1;
            removed++;
        }

        return removed;
    }

    // Rule-INTERNAL-090, Rule-INTERNAL-083
    private int stampIds(final @NotNull Path project, final @NotNull List<String> toRepair) {
        final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);
        int stamped = 0;

        for (final Path markerFile : markerFiles(project)) {
            final @NotNull Optional<DirectoryType> kind = DirectoryType.byMarker(String.valueOf(markerFile.getFileName()));
            if (kind.isEmpty()) continue;

            final @NotNull AbstractMarker marker;
            try {
                marker = mapper.readValue(markerFile.toFile(), kind.orElseThrow().getMarkerClass());
            } catch (final Exception unreadable) {
                Logger.warn("Left " + project.relativize(markerFile) + " without an id: " + unreadable.getMessage());
                toRepair.add(String.valueOf(project.relativize(markerFile)));
                continue;
            }

            if (!marker.getId().isEmpty()) continue;

            marker.setId(derived(placeOf(project, markerFile) + "|" + marker.getCreatedAt()).toString());
            if (!Services.getInstance(p, TestDataFiles.class).write(p, markerFile, marker)) return -1;

            stamped++;
        }

        return stamped;
    }

    private void stampFormat(final @NotNull Path markerFile) {
        readProjectMarker(markerFile).ifPresent(marker -> {
            marker.setFormat(TestProjectMarker.FORMAT);

            if (Services.getInstance(p, TestDataFiles.class).write(p, markerFile, marker)) {
                Logger.info("Converted " + markerFile.getParent().getFileName() + " to format " + TestProjectMarker.FORMAT);
            }
        });
    }

    private @NotNull Optional<TestProjectMarker> readProjectMarker(final @NotNull Path markerFile) {
        if (!Files.exists(markerFile)) return Optional.empty();

        try {
            return Optional.of(Services.getInstance(p, Mapper.class).readValue(markerFile.toFile(), TestProjectMarker.class));
        } catch (final Exception unreadable) {
            return Optional.empty();
        }
    }

    private static @NotNull List<Path> oldCaseFiles(final @NotNull Path testCases) {
        try (Stream<Path> walk = Files.walk(testCases)) {
            return walk.filter(Files::isRegularFile)
                    .filter(file -> String.valueOf(file.getFileName()).endsWith(".json"))
                    .filter(file -> !isGitsOwn(testCases, file))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (final IOException ex) {
            Logger.warn("Could not walk " + testCases + ": " + ex.getMessage());
            return List.of();
        }
    }

    private static @NotNull List<Path> oldRunFolders(final @NotNull Path testRuns) {
        try (Stream<Path> walk = Files.walk(testRuns)) {
            return walk.filter(Files::isDirectory)
                    .filter(folder -> Files.isRegularFile(folder.resolve("run.json")))
                    .filter(folder -> Files.exists(folder.resolve(DirectoryType.TR.getMarker()))
                            || Files.exists(folder.resolve(DirectoryType.TRP.getMarker())))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (final IOException ex) {
            Logger.warn("Could not walk " + testRuns + ": " + ex.getMessage());
            return List.of();
        }
    }

    private static @NotNull List<Path> markerFiles(final @NotNull Path project) {
        try (Stream<Path> walk = Files.walk(project)) {
            return walk.filter(Files::isRegularFile)
                    .filter(file -> !isGitsOwn(project, file))
                    .filter(file -> FileKind.of(file) == FileKind.MARKER)
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (final IOException ex) {
            Logger.warn("Could not walk " + project + ": " + ex.getMessage());
            return List.of();
        }
    }

    private static boolean isGitsOwn(final @NotNull Path root, final @NotNull Path file) {
        for (final Path segment : root.relativize(file)) {
            if (String.valueOf(segment).equals(".git")) return true;
        }

        return false;
    }

    // Rule-INTERNAL-012
    private @NotNull Optional<UUID> identityOf(final @NotNull Path file) {
        try {
            return Optional.of(UUID.fromString(baseName(file)));
        } catch (final IllegalArgumentException notAnId) {
            try {
                return Optional.of(Services.getInstance(p, Mapper.class).readValue(file.toFile(), TestCaseDto.class).getId());
            } catch (final Exception unreadable) {
                Logger.warn("Converting " + file.getFileName() + " under its own name: " + unreadable.getMessage());
                return Optional.empty();
            }
        }
    }

    private boolean reidentify(final @NotNull Path file, final @NotNull UUID fresh) {
        try {
            final @NotNull TestCaseDto tc = Services.getInstance(p, Mapper.class).readValue(file.toFile(), TestCaseDto.class);
            tc.setId(fresh);

            return Services.getInstance(p, TestDataFiles.class).write(p, file, tc);
        } catch (final Exception ex) {
            Logger.error("Could not give " + file.getFileName() + " an id of its own: " + ex.getMessage());
            return false;
        }
    }

    private boolean move(final @NotNull Path from, final @NotNull Path to) {
        return from.equals(to) || Services.getInstance(p, TestDataFiles.class).move(p, from, to);
    }

    private static @NotNull UUID derived(final @NotNull String from) {
        return UUID.nameUUIDFromBytes(from.getBytes(StandardCharsets.UTF_8));
    }

    // Rule-INTERNAL-082
    private static @NotNull String placeOf(final @NotNull Path project, final @NotNull Path file) {
        return String.valueOf(project.relativize(file)).replace(File.separatorChar, '/');
    }

    private static @NotNull String baseName(final @NotNull Path file) {
        final @NotNull String name = String.valueOf(file.getFileName());
        final int dot = name.lastIndexOf('.');

        return dot < 0 ? name : name.substring(0, dot);
    }
}
