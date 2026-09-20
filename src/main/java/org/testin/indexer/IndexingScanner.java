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

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.DirectoryType;
import org.testin.model.FileKind;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.dto.dirs.TestRunPackageDirectoryDto;
import org.testin.model.dto.dirs.TestRunsMainDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.model.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testcase.TestCaseOrder;
import org.testin.util.Bundle;
import org.testin.util.Mapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
final class IndexingScanner {

    private final @NotNull Project p;
    private final @NotNull IndexerDataStore store;

    void scanProject(final @NotNull Path projectPath, final @NotNull ProgressIndicator indicator) {
        try {
            scanProjectContents(projectPath, indicator);
        } finally {
            store.invalidateChildrenIndex();
        }
    }

    void scanProject(final @NotNull Path projectPath) {
        try {
            scanProjectContents(projectPath, new EmptyProgressIndicator());
        } finally {
            store.invalidateChildrenIndex();
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-005, Rule-INTERNAL-007, Rule-INTERNAL-091
    private void scanProjectContents(final @NotNull Path projectPath, final @NotNull ProgressIndicator indicator) {
        try {
            // UC-INTERNAL-008, Rule-INTERNAL-091.
            //
            // Before the project is read, and where both scan paths meet: a
            // project opened at startup, one the tester picks, one #301's clone
            // brings in, and a rescan after a copy or a restore all arrive here
            // (#305, G14). A project already in this build's format is not
            // touched.
            Services.getInstance(Conversions.class).ensure(p, projectPath);

            final @NotNull TestProjectDirectoryDto tp = Services.getInstance(p, DirectoryMapper.class).getTestProjectNode(p, projectPath);

            // Rule-INTERNAL-091. Written by an older Testin and not converted -
            // a conversion that failed - or by a newer one whose format this
            // build does not know: the project is a node saying why, and nothing
            // in it is read or written. Reading a format this build does not
            // understand is how a build deletes what it cannot see (#305, S5).
            final @NotNull Optional<String> refused = tp.getMarker().whyNotReadable();
            if (refused.isPresent()) {
                Logger.warn("Not reading " + projectPath.getFileName() + ": " + refused.orElseThrow());

                store.refuse(projectPath, refused.orElseThrow());
                store.swapIn(projectPath, scannedNode(projectPath, tp));
                indicator.setFraction(1.0);
                return;
            }

            // UC-INTERNAL-003, Rule-INTERNAL-021.
            //
            // Read into a pass of its own and put in at the end, so the index
            // never stops holding a project that is on disk. What the scan did
            // not find is dropped by the swap, which is how a rescan forgets what
            // disappeared instead of only learning what arrived: the scan used to
            // put and never remove, and the one path that cleared was Refresh -
            // so a test set deleted by a Git pull or a branch switch stayed in
            // the tree with its cases still in global search, the
            // completion cache and every export, until the tester pressed the
            // button Rule-INTERNAL-021 exists so they do not have to (#66,
            // finding 68). It cleared by emptying the project out first, which is
            // what ScannedProject describes and #312's A1 cost.
            final @NotNull ScannedProject scanned = new ScannedProject();
            scanned.getProjects().put(projectPath.toString(), tp);
            store.readable(projectPath);

            // UC-TREE-PANEL-001, Rule-TREE-PANEL-100.
            //
            // The node, and nothing under it. An inactive project is not being
            // worked on, so reading its test sets, cases and runs is a directory
            // walk nobody asked for - but it is still a project, and the tree
            // says so by drawing it with "Inactive" beside its name. Still
            // swapped in, so going inactive drops what it held.
            if (!tp.getMarker().getStatus().isActive()) {
                Logger.info("Inactive project, indexed without its contents: " + projectPath.getFileName());
                store.swapIn(projectPath, scanned);
                indicator.setFraction(1.0);
                return;
            }

                indicator.setFraction(0.1);
                indicator.setText(Bundle.message("indexer.progress.test.sets", tp.getName()));

            // Per scan, not a field: one scanner is built per project and reused
            // for every rescan, so a field would carry the last pass's folders
            // into this one.
            final @NotNull List<Path> unread = new ArrayList<>();

            final @NotNull TestCasesMainDirectoryDto tcd = tp.getTestCasesDirectory();
            scanned.getTestCasesMainDirs().put(tcd.getPath().toString(), tcd);
            scanTestSets(tcd.getPath(), tcd, indicator, unread, scanned);

                indicator.setFraction(0.5);
                indicator.setText(Bundle.message("indexer.progress.test.runs", tp.getName()));

            final @NotNull TestRunsMainDirectoryDto trd = tp.getTestRunsDirectory();
            scanned.getTestRunsMainDirs().put(trd.getPath().toString(), trd);
            scanTestRunDirs(trd.getPath(), trd, indicator, unread, scanned);

            // A pass the tester stopped read part of the project, and putting
            // that in would delete everything the walk had not reached yet. The
            // index goes on holding what it held, which is what it held a moment
            // ago and is still on disk.
            if (indicator.isCanceled()) {
                Logger.info("Scan canceled, so the index was left as it was: " + projectPath.getFileName());
                return;
            }

            store.swapIn(projectPath, scanned);

                indicator.setFraction(1.0);
                indicator.setText(Bundle.message("indexer.progress.project.done", tp.getName()));

            reportUnread(tp.getName(), unread);
            reportDamaged(tp.getName(), Services.getInstance(p, ProjectIndexer.class).takeDamagedMarkers());
            reportUnreadableResults(tp.getName(), scanned.getUnreadableResults());
            reportClashing(tp.getName(), List.copyOf(scanned.getClashingCases()));

        // Nothing is swapped in, for the same reason a canceled pass is not: a
        // scan that threw halfway read half a project, and the half it did not
        // reach is not gone from disk.
        } catch (final Exception ex) {
            Logger.error("Failed to scan project: " + projectPath.getFileName() + " - " + ex.getMessage());
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-008, Rule-INTERNAL-015
    private void scanTestSets(final @NotNull Path tcDir, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread, final @NotNull ScannedProject scanned) {
        try (Stream<Path> paths = Files.list(tcDir)) {
            final @NotNull List<Path> dirs = paths.filter(Files::isDirectory).toList();

            for (final Path dirPath : dirs) {
                // Between test sets, because that is where the tester's Cancel
                // has to land: a project is thousands of files and the pass is
                // long enough to want stopping. Asked rather than thrown -
                // stopping is an answer, so there is no exception for every
                // caller above to sort back out from a real failure.
                if (indicator.isCanceled()) return;

                store.markedAs(dirPath, DirectoryType.UNDER_TEST_CASES).ifPresentOrElse(
                        marked -> {
                            if (marked == DirectoryType.TS) scanTestSet(dirPath, parent, indicator, scanned);
                            else scanTestSetPackage(dirPath, parent, indicator, unread, scanned);
                        },
                        () -> skipped(dirPath, DirectoryType.UNDER_TEST_CASES, "test cases", unread));
            }
        } catch (final Exception ex) {
            Logger.error("Failed to list test sets: " + ex.getMessage());
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-008, Rule-INTERNAL-015
    private void scanTestSetPackage(final @NotNull Path path, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread, final @NotNull ScannedProject scanned) {
        try {
            final @NotNull DirectoryMapper dirMapper = Services.getInstance(p, DirectoryMapper.class);
            final @NotNull TestSetPackageDirectoryDto tsp = dirMapper.getTestSetPackageNode(p, path, parent);

            scanned.getTestSetPackages().put(path.toString(), tsp);

            try (Stream<Path> subPaths = Files.list(path)) {
                subPaths.filter(Files::isDirectory)
                        .forEach(subPath -> {
                            // The else was missing here, so a folder skipped one
                            // level down said nothing at all, not even to the log.
                            store.markedAs(subPath, DirectoryType.UNDER_TEST_CASES).ifPresentOrElse(marked -> {
                                if (marked == DirectoryType.TS) scanTestSet(subPath, tsp, indicator, scanned);
                                else scanTestSetPackage(subPath, tsp, indicator, unread, scanned);
                            }, () -> skipped(subPath, DirectoryType.UNDER_TEST_CASES, "test cases", unread));
                        });
            }

        } catch (final Exception ex) {
            Logger.error("Failed to scan test set package: " + path.getFileName());
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-011
    private void scanTestSet(final @NotNull Path path, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull ScannedProject scanned) {
        try {
            final @NotNull DirectoryMapper dirMapper = Services.getInstance(p, DirectoryMapper.class);
            final @NotNull TestSetDirectoryDto ts = dirMapper.getTestSetNode(p, path, parent);

            scanned.getTestSets().put(path.toString(), ts);

            final @NotNull List<UUID> caseIds = TestCaseSequenceStore.caseIds(List.of());
            final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);

            try (Stream<Path> files = Files.list(path)) {
                files.filter(Files::isRegularFile)
                        .filter(file -> FileKind.of(file) == FileKind.TEST_CASE)
                        .parallel()
                        .forEach(filePath -> {
                            try {
                                final @NotNull TestCaseDto tc = mapper.readValue(filePath.toFile(), TestCaseDto.class);
                                tc.setParent(ts);
                                tc.setId(identityOf(filePath, tc));

                                // Said rather than silently kept. The index holds
                                // one case per identity, so the second file of a
                                // pair goes over the first and both sets then
                                // resolve that id to whichever landed last
                                // (#312, A3).
                                if (scanned.getTestCasesById().put(tc.getId(), tc) != null) {
                                    scanned.getClashingCases().add(ts.getName() + "/" + filePath.getFileName());
                                    scanned.getClashingIds().add(tc.getId());
                                }

                                // Rule-INTERNAL-084. Remembered, so the save that
                                // files it under its id takes this one away.
                                if (!filePath.equals(TestCaseSequenceStore.named(path, tc.getId()))) {
                                    scanned.getHandNamedFiles().put(tc.getId(), filePath);
                                }

                                caseIds.add(tc.getId());
                            } catch (final Exception ex) {
                                Logger.error("Failed to read test case '" + filePath.toAbsolutePath() +
                                        "': " + ex.getMessage());

                                // Kept, so an export can name it without walking
                                // the folder itself (#66, finding 278).
                                scanned.getUnreadableCases().computeIfAbsent(path.toString(), ignored -> ConcurrentHashMap.newKeySet())
                                        .add(filePath.getFileName().toString());
                            }
                        });
            }

            scanned.getTestSetCaseIds().put(path.toString(), caseIds);

            indicator.setText(Bundle.message("indexer.progress.test.set", ts.getName(), String.valueOf(caseIds.size())));

        } catch (final Exception ex) {
            Logger.error("Failed to scan test set '" +
                    path.getFileName().toString() + "': " + ex.getMessage());
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-010, Rule-INTERNAL-015
    private void scanTestRunDirs(final @NotNull Path trDir, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread, final @NotNull ScannedProject scanned) {
        try (Stream<Path> paths = Files.list(trDir)) {
            final @NotNull List<Path> dirs = paths.filter(Files::isDirectory).toList();

            for (final Path dirPath : dirs) {
                // The same stopping point on the run side, for the same reason.
                if (indicator.isCanceled()) return;

                store.markedAs(dirPath, DirectoryType.UNDER_TEST_RUNS).ifPresentOrElse(
                        marked -> {
                            if (marked == DirectoryType.TR) scanTestRun(dirPath, parent, indicator, scanned);
                            else scanTestRunPackageDir(dirPath, parent, indicator, unread, scanned);
                        },
                        () -> skipped(dirPath, DirectoryType.UNDER_TEST_RUNS, "test runs", unread));
            }
        } catch (final Exception ex) {
            Logger.error("Failed to list test runs: " + ex.getMessage());
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-010, Rule-INTERNAL-015
    private void scanTestRunPackageDir(final @NotNull Path path, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread, final @NotNull ScannedProject scanned) {
        try {
            final @NotNull DirectoryMapper dirMapper = Services.getInstance(p, DirectoryMapper.class);
            final @NotNull TestRunPackageDirectoryDto trp = dirMapper.getTestRunPackageNode(p, path, parent);

            scanned.getTestRunPackages().put(path.toString(), trp);

            try (Stream<Path> subPaths = Files.list(path)) {
                subPaths.filter(Files::isDirectory)
                        .forEach(subPath -> {
                            // The else was missing here too.
                            store.markedAs(subPath, DirectoryType.UNDER_TEST_RUNS).ifPresentOrElse(marked -> {
                                if (marked == DirectoryType.TR) scanTestRun(subPath, trp, indicator, scanned);
                                else scanTestRunPackageDir(subPath, trp, indicator, unread, scanned);
                            }, () -> skipped(subPath, DirectoryType.UNDER_TEST_RUNS, "test runs", unread));
                        });
            }

        } catch (final Exception ex) {
            Logger.error("Failed to scan test run package: " + path.getFileName());
        }
    }

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-015.
     * <p>
     * A folder with no marker is not a node, so the scan cannot read it or
     * anything under it. Most of them are nothing: a folder somebody made beside
     * the test sets, a working directory, something a tool left behind. Those are
     * the ordinary case and saying anything about them would be noise.
     * <p>
     * A folder holding test cases is not the ordinary case. Those cases are on
     * disk and in no panel, no search, no report and no export, and until now the
     * only trace was one line in a log nothing points at - four of them sat in the
     * sandbox project that way, and one in a real data root (#276).
     * <p>
     * So the log line is kept for every skip, and the folder is remembered only
     * when it holds something the tester would miss.
     */
    private void skipped(final @NotNull Path dirPath, final @NotNull List<DirectoryType> family, final @NotNull String where, final @NotNull List<Path> unread) {
        Logger.warn("Skipping unmarked directory under " + where + " (missing " + DirectoryType.markerNames(family) + "): " + dirPath);

        if (holdsTestCases(dirPath)) unread.add(dirPath);
    }

    /**
     * One listing of a folder the scan was about to throw away, so it costs
     * nothing on the folders that are really nodes.
     */
    private boolean holdsTestCases(final @NotNull Path dirPath) {
        try (Stream<Path> files = Files.list(dirPath)) {
            return files.filter(Files::isRegularFile).anyMatch(IndexingScanner::looksLikeACaseFile);
        } catch (final Exception unreadable) {
            // A folder that will not even list is a bigger problem than a missing
            // marker, and the line above already said the scan skipped it.
            return false;
        }
    }

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-015.
     * <p>
     * One notification for the whole project, not one per folder, and it stays in
     * the log rather than fading: a scan finishes on its own time, and a balloon
     * that fades while the tester is reading something else is no better than the
     * silence it replaced.
     */
    private void reportUnread(final @NotNull String projectName, final @NotNull List<Path> unread) {
        if (unread.isEmpty()) return;

        final @NotNull String named = unread.stream().limit(5).map(path -> path.getFileName().toString()).collect(Collectors.joining(", "));
        final @NotNull String rest = unread.size() > 5
                ? Bundle.message("indexer.more", String.valueOf(unread.size() - 5))
                : "";
        final @NotNull String count = unread.size() == 1
                ? Bundle.message("indexer.unread.one")
                : Bundle.message("indexer.unread.many", String.valueOf(unread.size()));

        Services.getInstance(p, Notifier.class).warn(p, Bundle.message("indexer.unread.title", projectName),
                Bundle.message("indexer.unread.message", count, named, rest));
    }

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-011.
     * <p>
     * The results that would not parse, once for the project rather than once per
     * file. The run is shown without them, and nothing writes over them or
     * removes them, so the tester can repair the file and press Refresh (#305,
     * S21).
     */
    private void reportUnreadableResults(final @NotNull String projectName, final @NotNull Set<String> unreadable) {
        if (unreadable.isEmpty()) return;

        final @NotNull String named = unreadable.stream().sorted().limit(5).collect(Collectors.joining(", "));
        final @NotNull String rest = unreadable.size() > 5
                ? Bundle.message("indexer.more", String.valueOf(unreadable.size() - 5))
                : "";
        final @NotNull String count = unreadable.size() == 1
                ? Bundle.message("indexer.results.unread.one")
                : Bundle.message("indexer.results.unread.many", String.valueOf(unreadable.size()));

        Services.getInstance(p, Notifier.class).warn(p, Bundle.message("indexer.results.unread.title", projectName),
                Bundle.message("indexer.results.unread.message", count, named, rest));
    }

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-014.
     * <p>
     * A marker that is there and will not parse leaves its node drawn with
     * default values: its number, its status and who made it are not what the
     * file says, and nothing about the node on screen shows it. One thing that
     * cannot be read never stops the rest, which is why the node is still drawn -
     * but a node quietly wrong is worse than one that says so (#277).
     * <p>
     * One notification for the project, like the folders above, and it stays in
     * the list rather than fading: a marker is repaired by hand, and the tester
     * needs the names after the balloon would have gone.
     */
    private void reportDamaged(final @NotNull String projectName, final @NotNull List<String> damaged) {
        if (damaged.isEmpty()) return;

        final @NotNull String named = damaged.stream().limit(5).collect(Collectors.joining(", "));
        final @NotNull String rest = damaged.size() > 5
                ? Bundle.message("indexer.more", String.valueOf(damaged.size() - 5))
                : "";
        final @NotNull String count = damaged.size() == 1
                ? Bundle.message("indexer.damaged.one")
                : Bundle.message("indexer.damaged.many", String.valueOf(damaged.size()));

        Services.getInstance(p, Notifier.class).warn(p, Bundle.message("indexer.damaged.title", projectName),
                Bundle.message("indexer.damaged.message", count, named, rest));
    }

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-082.
     * <p>
     * The test case files whose identity another file had already taken, named
     * once for the project.
     * <p>
     * The same shape as the two above it, and for the same reason: the tester
     * repairs this by renaming a file, and needs the names after a balloon would
     * have faded. What it cannot do is choose which of the pair keeps the
     * identity, so it says which files collided and leaves that to them.
     */
    private void reportClashing(final @NotNull String projectName, final @NotNull List<String> clashing) {
        if (clashing.isEmpty()) return;

        final @NotNull String named = clashing.stream().sorted().limit(5).collect(Collectors.joining(", "));
        final @NotNull String rest = clashing.size() > 5
                ? Bundle.message("indexer.more", String.valueOf(clashing.size() - 5))
                : "";
        final @NotNull String count = clashing.size() == 1
                ? Bundle.message("indexer.clash.one")
                : Bundle.message("indexer.clash.many", String.valueOf(clashing.size()));

        Services.getInstance(p, Notifier.class).warn(p, Bundle.message("indexer.clash.title", projectName),
                Bundle.message("indexer.clash.message", count, named, rest));
    }

    // UC-INTERNAL-002
    private void scanTestRun(final @NotNull Path path, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull ScannedProject scanned) {
        try {
            final @NotNull DirectoryMapper dirMapper = Services.getInstance(p, DirectoryMapper.class);
            final @NotNull TestRunDirectoryDto tr = dirMapper.getTestRunNode(p, path, parent);

            scanned.getTestRunDirs().put(path.toString(), tr);

            // Rule-INTERNAL-011. Registered for every {@code .tr}, whatever its
            // folder holds: a run is a run before its first verdict and after its
            // last case is unticked, and one registered only when a results file
            // existed could not be edited again (#305, S21).
            scanned.getTestRuns().put(path.toString(), new TestRunDto().setResults(resultsIn(path, scanned)));

            indicator.setText(Bundle.message("indexer.progress.test.run", path.getFileName()));

        } catch (final Exception ex) {
            Logger.error("Failed to scan test run '" +
                    path.getFileName().toString() + "': " + ex.getMessage());
        }
    }

    /**
     * Whether an unmarked folder's file looks like a test case, so the warning
     * can say how many it is passing over.
     * <p>
     * A guess on purpose, and the only place one is made. The folder carries no
     * marker, so there is no test set to ask - which is exactly what the warning
     * is about. A name Testin wrote is the best evidence available; a run's file
     * is named for its folder and does not match.
     * <p>
     * Deliberately not the same question as {@code ProjectIndexer.isCaseFile},
     * which asks whether a file <b>is</b> a test case and answers it by the rule
     * - a {@code .tc} directly inside a test set. The two shared one method
     * until #288, and the sharing is what hid that they were asking different
     * things.
     */
    private static boolean looksLikeACaseFile(final @NotNull Path file) {
        return FileKind.TEST_CASE.idIn(file).isPresent();
    }

    /**
     * Rule-INTERNAL-091.
     * <p>
     * The project as a node and nothing else, which is what a refused project is
     * indexed as - the same shape an inactive one takes, so the tree can say what
     * it is rather than leaving the tester with an empty panel (#305, S9).
     */
    private static @NotNull ScannedProject scannedNode(final @NotNull Path projectPath, final @NotNull TestProjectDirectoryDto tp) {
        final @NotNull ScannedProject scanned = new ScannedProject();
        scanned.getProjects().put(projectPath.toString(), tp);

        return scanned;
    }

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-011, Rule-INTERNAL-012.
     * <p>
     * A run's results, one {@code <test case id>.ri} each, in the order their
     * cases sit in their test sets - the order the run editor draws and every
     * report prints, so a directory listing's own order is never what a tester
     * sees (#305, S19). A result whose case this project no longer holds keeps
     * its verdict and comes last.
     * <p>
     * The file name is the identity, as it is for a test case
     * (Rule-INTERNAL-012): the id inside is read back only to be replaced by it.
     * <p>
     * A file that will not parse is reported and left where it is - never written
     * over, never removed, and never read as a case nobody judged (#305, S21).
     */
    private @NotNull List<TestRunItems> resultsIn(final @NotNull Path runPath, final @NotNull ScannedProject scanned) {
        final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);
        final @NotNull List<TestRunItems> read = new ArrayList<>();

        // Which files in the folder are results is the file owner's answer, not a
        // second listing here: one filter, one failure to report (#305).
        for (final Path file : Services.getInstance(p, TestDataFiles.class).resultsIn(runPath)) {
            try {
                final @NotNull TestRunItems item = mapper.readValue(file.toFile(), TestRunItems.class);
                FileKind.RUN_ITEM.idIn(file).ifPresent(item::setId);
                read.add(item);

            } catch (final Exception ex) {
                Logger.error("Failed to read the result '" + file.toAbsolutePath() + "': " + ex.getMessage());
                scanned.getUnreadableResults().add(runPath.getFileName() + "/" + file.getFileName());
            }
        }

        return inCaseOrder(read, scanned);
    }

    /**
     * Rule-INTERNAL-011.
     * <p>
     * The results in their cases' own order, and the ones whose case this project
     * does not hold after them - a run outlives the cases it was made from, and
     * what it recorded about a removed one is still its record (#305, S19).
     */
    private static @NotNull List<TestRunItems> inCaseOrder(final @NotNull List<TestRunItems> results, final @NotNull ScannedProject scanned) {
        final @NotNull Map<UUID, TestRunItems> byId = new LinkedHashMap<>();
        results.forEach(item -> byId.put(item.getId(), item));

        final @NotNull List<TestCaseDto> cases = results.stream()
                .map(item -> scanned.getTestCasesById().get(item.getId()))
                .filter(Objects::nonNull)
                .toList();

        final @NotNull List<TestRunItems> ordered = TestCaseOrder.ordered(cases).stream()
                .map(tc -> byId.remove(tc.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        ordered.addAll(byId.values());
        return ordered;
    }

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-012.
     * <p>
     * Which test case a file is: its name, when the name is a UUID.
     * <p>
     * The plugin writes a case to {@code <id>.tc} and reads it back keyed by
     * the id inside, so the two always agree - until a file is copied outside
     * the plugin, which is a thing people do on GitHub. Then two files claim one
     * id, the cache keeps whichever the parallel scan reached last, and the other
     * case is gone. Worse than gone: when the file that lost was the one holding
     * {@code isHead}, the set has no starting point at all and every case in it
     * shows as unsorted.
     * <p>
     * The name is the identity because it cannot collide - one directory cannot
     * hold two files with the same name - so a copied file becomes a second case
     * rather than a coin toss. It arrives pointed at by nothing, which is what
     * the Unsorted badge is for.
     * <p>
     * A name that is not a UUID keeps the id inside the file: that is a file the
     * plugin did not write, and inventing an identity for it would be worse than
     * believing what it says.
     */
    private static @NotNull UUID identityOf(final @NotNull Path filePath, final @NotNull TestCaseDto tc) {
        final @NotNull Optional<UUID> fromTheName = FileKind.TEST_CASE.idIn(filePath);

        if (fromTheName.isPresent()) {
            final @NotNull UUID fromName = fromTheName.orElseThrow();

            if (!fromName.equals(tc.getId())) {
                Logger.warn("Test case " + filePath.getFileName() + " says its id is " + tc.getId()
                        + "; the file name is the identity, so it is read as " + fromName);
            }
            return fromName;
        }

        return tc.getId();
    }
}
