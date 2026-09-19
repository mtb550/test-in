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

/**
 * UC-INTERNAL-008, Rule-INTERNAL-091.
 * <p>
 * Brings one test project's files to the format this build reads, once, and
 * holds <b>everything</b> Testin still knows about the format before it: test
 * cases as {@code <uuid>.json}, a run as one {@code run.json}, and markers with
 * no id (#305, D4).
 * <p>
 * <b>2.14.0-alpha deletes this class</b>, and with it the last code that can read
 * the old format. What stays is the check on {@link TestProjectMarker#getFormat},
 * which then refuses a project nobody converted and names the release that can.
 * That is why the number is written last, and only when every step succeeded: a
 * conversion stopped half way - the IDE closed, a file locked - is finished by the
 * next open rather than declared done.
 * <p>
 * Existing test runs are removed rather than converted (D4). The results of a run
 * are what two testers disagreed over in one file; carrying that shape forward is
 * the thing this format exists to end, and Muteb's decision is that a run is
 * cheap to re-create and a converter for it is not. The release notes say so
 * before anyone installs it.
 * <p>
 * <b>Every machine converting the same commit writes the same bytes</b> (S1): an
 * id it stamps is derived from the marker's own path inside the project and when
 * that folder was created, never from a random source, so a shared project
 * converted by two testers merges instead of conflicting on every marker.
 */
@AllArgsConstructor
final class FormatConverter {

    private final @NotNull Project p;

    /**
     * What one conversion did, for the one notification the sweep raises.
     *
     * @param project   the project's folder name
     * @param cases     how many test case files were renamed
     * @param runs      how many old-format runs were removed
     * @param ids       how many folders were given an id
     * @param toRepair  the files left as they are for the tester to repair - a
     *                  case or a marker that would not parse
     * @param failed    whether a step failed, which keeps the format number out
     *                  of the marker so the next open tries again
     */
    record Report(@NotNull String project, int cases, int runs, int ids, @NotNull List<String> toRepair, boolean failed) {

        boolean changedAnything() {
            return cases > 0 || runs > 0 || ids > 0;
        }
    }

    /**
     * UC-INTERNAL-008, Rule-INTERNAL-091.
     * <p>
     * The project at this path, in this build's format. A project already in it is
     * not read, not written and not reported.
     */
    @NotNull Optional<Report> convert(final @NotNull Path project) {
        final @NotNull Path markerFile = project.resolve(DirectoryType.TP.getMarker());
        final @NotNull String name = String.valueOf(project.getFileName());

        final @NotNull Optional<TestProjectMarker> marker = readProjectMarker(markerFile);
        if (marker.isEmpty()) {
            // A .tp that is there and will not parse: nothing in the project is
            // touched, and the tester is told which file to repair (#305, S3).
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

    /**
     * Rule-INTERNAL-011, Rule-INTERNAL-084.
     * <p>
     * Every {@code .json} directly inside a folder under {@code Test Cases}
     * becomes {@code <its id>.tc}, marked or not, and whether the project is
     * active or not: a folder with no marker and an inactive project are both read
     * again later, and a file left as {@code .json} would never be read again
     * (#305, S7).
     * <p>
     * The content is untouched and the file is <b>moved</b>, so a crash between the
     * two leaves one file rather than two (S11). A file that will not parse keeps
     * its base name - {@code login.json} becomes {@code login.tc} - so the scan
     * goes on reporting it instead of losing sight of it.
     *
     * @return how many were converted, or -1 when a step failed
     */
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

            // Rule-INTERNAL-082, #288. Two files claiming one id: the first path
            // alphabetically keeps it, and the second takes one derived from the
            // id it claimed and its own path - the same on every machine (S1).
            final @NotNull UUID own = taken.add(id.orElseThrow())
                    ? id.orElseThrow()
                    : derived(id.orElseThrow() + "|" + project.relativize(file));

            if (!own.equals(id.orElseThrow()) && !reidentify(file, own)) return -1;
            if (!move(file, file.resolveSibling(FileKind.TEST_CASE.fileName(own)))) return -1;

            taken.add(own);
            converted++;
        }

        return converted;
    }

    /**
     * D4. Every old-format run goes: a marked run or run package holding a
     * {@code run.json}, with its results and its screenshots.
     * <p>
     * Only those. A folder under {@code Test Runs} that Testin did not write - a
     * tester's own notes, a screenshot beside them - is left where it is, because
     * the converter removes what it recognizes rather than what it finds (S2).
     *
     * @return how many runs were removed, or -1 when a removal failed
     */
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

    /**
     * Rule-INTERNAL-090, Rule-INTERNAL-083.
     * <p>
     * Every marker that parses and has no id takes one, derived from its path
     * inside the project and when the folder was created - so two machines
     * converting the same commit write the same id and Git has nothing to
     * conflict over (S1). The audit block is not touched.
     * <p>
     * A marker that will not parse is left exactly as it is and named for the
     * tester to repair: writing an id into it would replace the file with
     * defaults (S13).
     *
     * @return how many were stamped, or -1 when a write failed
     */
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

            marker.setId(derived(project.relativize(markerFile) + "|" + marker.getCreatedAt()).toString());
            if (!Services.getInstance(p, TestDataFiles.class).write(p, markerFile, marker)) return -1;

            stamped++;
        }

        return stamped;
    }

    /**
     * D4. The format number, last and on the marker as it reads now - not on the
     * copy read before the steps, which a status change from another window may
     * have replaced since (S13).
     */
    private void stampFormat(final @NotNull Path markerFile) {
        readProjectMarker(markerFile).ifPresent(marker -> {
            marker.setFormat(TestProjectMarker.FORMAT);

            if (Services.getInstance(p, TestDataFiles.class).write(p, markerFile, marker)) {
                Logger.info("Converted " + markerFile.getParent().getFileName() + " to format " + TestProjectMarker.FORMAT);
            }
        });
    }

    /**
     * The project's marker, and empty when the file is there and will not parse.
     * A project with no {@code .tp} at all is not a test project, and the scan
     * never asks about one.
     */
    private @NotNull Optional<TestProjectMarker> readProjectMarker(final @NotNull Path markerFile) {
        if (!Files.exists(markerFile)) return Optional.of(new TestProjectMarker());

        try {
            return Optional.of(Services.getInstance(p, Mapper.class).readValue(markerFile.toFile(), TestProjectMarker.class));
        } catch (final Exception unreadable) {
            return Optional.empty();
        }
    }

    /**
     * The old format's test case files: every {@code .json} one level inside a
     * folder under {@code Test Cases}, in path order so two machines convert a
     * pair claiming one id the same way round (S1).
     */
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

    /**
     * The folders that hold an old-format run: one Testin marked as a run or a run
     * package, holding a {@code run.json}.
     */
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

    /**
     * Every marker in the project, its own included, and never one inside
     * {@code .git}: a repository's own directory is not test data (S12).
     */
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

    /**
     * Rule-INTERNAL-012. Which case an old file was: its name when that was a
     * uuid, the id inside it otherwise, and nothing when it will not parse at all.
     */
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

    /**
     * The case under another id, written before its file moves so the two never
     * disagree.
     */
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

    /**
     * S1. The same id on every machine, from something about the file rather than
     * from chance: a uuid over the text handed in.
     */
    private static @NotNull UUID derived(final @NotNull String from) {
        return UUID.nameUUIDFromBytes(from.getBytes(StandardCharsets.UTF_8));
    }

    private static @NotNull String baseName(final @NotNull Path file) {
        final @NotNull String name = String.valueOf(file.getFileName());
        final int dot = name.lastIndexOf('.');

        return dot < 0 ? name : name.substring(0, dot);
    }
}
