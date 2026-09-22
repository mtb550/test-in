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
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
final class IndexingScanner {
    private static final int SHOWN = 5;
    private final @NotNull Project p;
    private final @NotNull IndexerDataStore store;

    private static boolean looksLikeATestCaseFile(final @NotNull Path file) {
        return FileKind.TEST_CASE.idIn(file).isPresent();
    }

    // Rule-INTERNAL-091
    private static @NotNull ScannedProject scannedNode(final @NotNull Path projectPath, final @NotNull TestProjectDirectoryDto tp) {
        final @NotNull ScannedProject scanned = new ScannedProject();
        scanned.getProjects().put(projectPath.toString(), tp);

        return scanned;
    }

    // Rule-INTERNAL-011
    static @NotNull List<TestRunItems> inTestCaseOrder(final @NotNull List<TestRunItems> results, final @NotNull ScannedProject scanned) {
        final @NotNull Map<UUID, TestRunItems> byId = new LinkedHashMap<>();
        results.forEach(item -> byId.put(item.getId(), item));

        final @NotNull List<TestCaseDto> testCases = results.stream()
                .map(item -> scanned.getTestCasesById().get(item.getId()))
                .filter(Objects::nonNull)
                .toList();

        final @NotNull List<TestRunItems> ordered = TestCaseOrder.ordered(testCases).stream()
                .map(tc -> byId.remove(tc.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        ordered.addAll(byId.values());
        return ordered;
    }

    // UC-INTERNAL-002, Rule-INTERNAL-012
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
            // UC-INTERNAL-008, Rule-INTERNAL-091
            Services.getInstance(Conversions.class).ensure(p, projectPath);

            final @NotNull TestProjectDirectoryDto tp = Services.getInstance(p, DirectoryMapper.class).getTestProjectNode(p, projectPath);

            // Rule-INTERNAL-091
            final @NotNull Optional<String> refused = tp.getMarker().whyNotReadable();
            if (refused.isPresent()) {
                Logger.warn("Not reading " + projectPath.getFileName() + ": " + refused.orElseThrow());

                store.refuse(projectPath, refused.orElseThrow());
                store.swapIn(projectPath, scannedNode(projectPath, tp));
                indicator.setFraction(1.0);
                return;
            }

            // UC-INTERNAL-003, Rule-INTERNAL-021
            // Rule-INTERNAL-021
            final @NotNull ScannedProject scanned = new ScannedProject();
            scanned.getProjects().put(projectPath.toString(), tp);
            store.readable(projectPath);

            // UC-TREE-PANEL-001, Rule-TREE-PANEL-100
            if (!tp.getMarker().getStatus().isActive()) {
                Logger.info("Inactive project, indexed without its contents: " + projectPath.getFileName());
                store.swapIn(projectPath, scanned);
                indicator.setFraction(1.0);
                return;
            }

            indicator.setFraction(0.1);
            indicator.setText(Bundle.message("indexer.progress.test.sets", tp.getName()));

            final @NotNull List<Path> unread = new ArrayList<>();

            final @NotNull TestCasesMainDirectoryDto tcd = tp.getTestCasesDirectory();
            scanned.getTestCasesMainDirs().put(tcd.getPath().toString(), tcd);
            scanTestSets(tcd.getPath(), tcd, indicator, unread, scanned);

            indicator.setFraction(0.5);
            indicator.setText(Bundle.message("indexer.progress.test.runs", tp.getName()));

            final @NotNull TestRunsMainDirectoryDto trd = tp.getTestRunsDirectory();
            scanned.getTestRunsMainDirs().put(trd.getPath().toString(), trd);
            scanTestRunDirs(trd.getPath(), trd, indicator, unread, scanned);

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
            reportHandNamedResults(tp.getName(), scanned.getHandNamedResults());
            reportClashing(tp.getName(), List.copyOf(scanned.getClashingTestCases()));

        } catch (final Exception ex) {
            Logger.error("Failed to scan project: " + projectPath.getFileName() + " - " + ex.getMessage());
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-008, Rule-INTERNAL-015
    private void scanTestSets(final @NotNull Path tcDir, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread, final @NotNull ScannedProject scanned) {
        try (Stream<Path> paths = Files.list(tcDir)) {
            final @NotNull List<Path> dirs = paths.filter(Files::isDirectory).toList();

            for (final Path dirPath : dirs) {
                if (indicator.isCanceled()) return;

                store.markedAs(dirPath, DirectoryType.UNDER_TEST_CASES).ifPresentOrElse(
                        marked -> {
                            if (marked == DirectoryType.TS) scanTestSet(dirPath, parent, indicator, scanned);
                            else scanTestSetPackage(dirPath, parent, indicator, unread, scanned);
                        },
                        () -> skipped(dirPath, DirectoryType.UNDER_TEST_CASES, unread));
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
                        .forEach(subPath -> store.markedAs(subPath, DirectoryType.UNDER_TEST_CASES).ifPresentOrElse(marked -> {
                            if (marked == DirectoryType.TS) scanTestSet(subPath, tsp, indicator, scanned);
                            else scanTestSetPackage(subPath, tsp, indicator, unread, scanned);
                        }, () -> skipped(subPath, DirectoryType.UNDER_TEST_CASES, unread)));
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

            final @NotNull List<UUID> testCaseIds = TestCaseSequenceStore.testCaseIds(List.of());
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

                                if (scanned.getTestCasesById().put(tc.getId(), tc) != null) {
                                    scanned.getClashingTestCases().add(ts.getName() + "/" + filePath.getFileName());
                                    scanned.getClashingIds().add(tc.getId());
                                }

                                // Rule-INTERNAL-084
                                if (!filePath.equals(TestCaseSequenceStore.named(path, tc.getId()))) {
                                    scanned.getHandNamedFiles().put(tc.getId(), filePath);
                                }

                                testCaseIds.add(tc.getId());
                            } catch (final Exception ex) {
                                Logger.error("Failed to read test case '" + filePath.toAbsolutePath() +
                                        "': " + ex.getMessage());

                                scanned.getUnreadableTestCases().computeIfAbsent(path.toString(), _ -> ConcurrentHashMap.newKeySet())
                                        .add(filePath.getFileName().toString());
                            }
                        });
            }

            scanned.getTestCaseIdsByTestSet().put(path.toString(), testCaseIds);

            indicator.setText(Bundle.message("indexer.progress.test.set", ts.getName(), String.valueOf(testCaseIds.size())));

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
                if (indicator.isCanceled()) return;

                store.markedAs(dirPath, DirectoryType.UNDER_TEST_RUNS).ifPresentOrElse(
                        marked -> {
                            if (marked == DirectoryType.TR) scanTestRun(dirPath, parent, indicator, scanned);
                            else scanTestRunPackageDir(dirPath, parent, indicator, unread, scanned);
                        },
                        () -> skipped(dirPath, DirectoryType.UNDER_TEST_RUNS, unread));
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
                        .forEach(subPath -> store.markedAs(subPath, DirectoryType.UNDER_TEST_RUNS).ifPresentOrElse(marked -> {
                            if (marked == DirectoryType.TR) scanTestRun(subPath, trp, indicator, scanned);
                            else scanTestRunPackageDir(subPath, trp, indicator, unread, scanned);
                        }, () -> skipped(subPath, DirectoryType.UNDER_TEST_RUNS, unread)));
            }

        } catch (final Exception ex) {
            Logger.error("Failed to scan test run package: " + path.getFileName());
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-015
    private void skipped(final @NotNull Path dirPath, final @NotNull List<DirectoryType> family, final @NotNull List<Path> unread) {
        Logger.warn("Skipping unmarked directory (missing " + DirectoryType.markerNames(family) + "): " + dirPath);

        if (holdsTestCases(dirPath)) unread.add(dirPath);
    }

    private boolean holdsTestCases(final @NotNull Path dirPath) {
        try (Stream<Path> files = Files.list(dirPath)) {
            return files.filter(Files::isRegularFile).anyMatch(IndexingScanner::looksLikeATestCaseFile);
        } catch (final Exception unreadable) {
            return false;
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-015
    private void reportUnread(final @NotNull String projectName, final @NotNull List<Path> unread) {
        final @NotNull List<String> names = unread.stream().map(path -> path.getFileName().toString()).toList();

        say(Bundle.message("indexer.unread.title", projectName), names,
                name -> Bundle.message("indexer.unread.one", name),
                (named, rest) -> Bundle.message("indexer.unread.many", String.valueOf(names.size()), named, rest));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-011
    private void reportUnreadableResults(final @NotNull String projectName, final @NotNull Set<String> unreadable) {
        final @NotNull List<String> names = unreadable.stream().sorted().toList();

        say(Bundle.message("indexer.results.unread.title", projectName), names,
                name -> Bundle.message("indexer.results.unread.one", name),
                (named, rest) -> Bundle.message("indexer.results.unread.many", String.valueOf(names.size()), named, rest));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-094
    private void reportHandNamedResults(final @NotNull String projectName, final @NotNull Set<String> handNamed) {
        final @NotNull List<String> names = handNamed.stream().sorted().toList();

        say(Bundle.message("indexer.results.unnamed.title", projectName), names,
                name -> Bundle.message("indexer.results.unnamed.one", name),
                (named, rest) -> Bundle.message("indexer.results.unnamed.many", String.valueOf(names.size()), named, rest));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-014
    private void reportDamaged(final @NotNull String projectName, final @NotNull List<String> damaged) {
        say(Bundle.message("indexer.damaged.title", projectName), damaged,
                name -> Bundle.message("indexer.damaged.one", name),
                (named, rest) -> Bundle.message("indexer.damaged.many", String.valueOf(damaged.size()), named, rest));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-015
    private void say(final @NotNull String title, final @NotNull List<String> names, final @NotNull UnaryOperator<String> one, final @NotNull BinaryOperator<String> many) {
        if (names.isEmpty()) return;

        if (names.size() == 1) {
            Services.getInstance(p, Notifier.class).warn(p, title, one.apply(names.getFirst()));
            return;
        }

        final @NotNull String named = names.stream().limit(SHOWN).collect(Collectors.joining(", "));
        final @NotNull String rest = names.size() > SHOWN
                ? Bundle.message("indexer.more", String.valueOf(names.size() - SHOWN))
                : "";

        Services.getInstance(p, Notifier.class).warn(p, title, many.apply(named, rest));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-082
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

            // Rule-INTERNAL-011
            scanned.getTestRuns().put(path.toString(), new TestRunDto().setResults(resultsIn(path, scanned)));

            indicator.setText(Bundle.message("indexer.progress.test.run", path.getFileName()));

        } catch (final Exception ex) {
            Logger.error("Failed to scan test run '" +
                    path.getFileName().toString() + "': " + ex.getMessage());
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-011, Rule-INTERNAL-012
    private @NotNull List<TestRunItems> resultsIn(final @NotNull Path runPath, final @NotNull ScannedProject scanned) {
        final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);
        final @NotNull List<TestRunItems> read = new ArrayList<>();

        for (final Path file : Services.getInstance(p, TestDataFiles.class).resultsIn(runPath)) {
            // Rule-INTERNAL-094
            final @NotNull Optional<UUID> id = FileKind.RUN_ITEM.idIn(file);
            if (id.isEmpty()) {
                scanned.getHandNamedResults().add(runPath.getFileName() + "/" + file.getFileName());
                continue;
            }

            try {
                final @NotNull TestRunItems item = mapper.readValue(file.toFile(), TestRunItems.class);
                item.setId(id.orElseThrow());
                read.add(item);

            } catch (final Exception ex) {
                Logger.error("Failed to read the result '" + file.toAbsolutePath() + "': " + ex.getMessage());
                scanned.getUnreadableResults().add(runPath.getFileName() + "/" + file.getFileName());
            }
        }

        return inTestCaseOrder(read, scanned);
    }
}
