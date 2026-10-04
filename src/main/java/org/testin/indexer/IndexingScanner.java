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
import org.testin.services.Services;
import org.testin.testcase.TestCaseOrder;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.Mapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class IndexingScanner {
    private final @NotNull Project p;
    private final @NotNull IndexerDataStore store;
    private final @NotNull DirectoryMapper directoryMapper;
    private final @NotNull Mapper mapper;
    private final @NotNull ReadProblems problems;
    private final @NotNull TestDataFiles testDataFiles;

    IndexingScanner(final @NotNull Project p, final @NotNull IndexerDataStore store) {
        this.p = p;
        this.store = store;
        this.directoryMapper = Services.getInstance(p, DirectoryMapper.class);
        this.mapper = Services.getInstance(p, Mapper.class);
        this.problems = new ReadProblems(p, store);
        this.testDataFiles = Services.getInstance(p, TestDataFiles.class);
    }

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

    // Rule-INTERNAL-124
    @NotNull Optional<String> scanProject(final @NotNull Path projectPath, final @NotNull ProgressIndicator indicator) {
        try {
            return scanProjectContents(projectPath, indicator);
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
    private @NotNull Optional<String> scanProjectContents(final @NotNull Path projectPath, final @NotNull ProgressIndicator indicator) {
        try {
            final @NotNull TestProjectDirectoryDto tp = directoryMapper.getTestProjectNode(projectPath);

            // Rule-INTERNAL-091
            final @NotNull Optional<String> refused = tp.getMarker().whyNotReadable();
            if (refused.isPresent()) {
                Logger.warn("Not reading " + projectPath.getFileName() + ": " + refused.orElseThrow());

                store.refuse(projectPath, refused.orElseThrow());
                store.swapIn(projectPath, scannedNode(projectPath, tp));
                indicator.setFraction(1.0);
                return Optional.empty();
            }

            // UC-INTERNAL-003, Rule-INTERNAL-021
            final @NotNull ScannedProject scanned = new ScannedProject();
            scanned.getProjects().put(projectPath.toString(), tp);
            store.readable(projectPath);

            // UC-TREE-PANEL-001, Rule-TREE-PANEL-100
            if (!tp.getMarker().getStatus().isActive()) {
                Logger.info("Inactive project, indexed without its contents: " + projectPath.getFileName());
                store.swapIn(projectPath, scanned);
                indicator.setFraction(1.0);
                return Optional.empty();
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
                return Optional.empty();
            }

            store.swapIn(projectPath, scanned);

            indicator.setFraction(1.0);
            indicator.setText(Bundle.message("indexer.progress.project.done", tp.getName()));

            problems.unreadFolders(tp.getName(), unread);
            problems.damagedMarkers(tp.getName(), store.takeDamagedMarkers(projectPath));
            problems.unreadableResults(tp.getName(), scanned.getUnreadableResults());
            problems.handNamedResults(tp.getName(), scanned.getHandNamedResults());
            problems.clashingTestCases(tp.getName(), List.copyOf(scanned.getClashingTestCases()));
            return Optional.empty();
        } catch (final Exception ex) {
            Logger.error("Failed to scan project: " + projectPath.getFileName() + " - " + FailureText.of(ex));
            return Optional.of(FailureText.of(ex));
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-008, Rule-INTERNAL-015
    private void scanTestSets(final @NotNull Path tcDir, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread, final @NotNull ScannedProject scanned) {
        try (Stream<Path> paths = Files.list(tcDir)) {
            final @NotNull List<Path> dirs = paths.filter(Files::isDirectory).toList();

            for (final Path dirPath : dirs) {
                if (indicator.isCanceled()) return;

                scanTestSetOrPackage(dirPath, parent, indicator, unread, scanned);
            }
        } catch (final NoSuchFileException absent) {
            Logger.info("No test sets to read: " + tcDir);
        } catch (final IOException ex) {
            throw new UncheckedIOException("the folder " + tcDir.getFileName() + " could not be listed: " + FailureText.of(ex), ex);
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-008, Rule-INTERNAL-015
    private void scanTestSetOrPackage(final @NotNull Path dirPath, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread, final @NotNull ScannedProject scanned) {
        store.markedAs(dirPath, DirectoryType.UNDER_TEST_CASES).ifPresentOrElse(
                marked -> {
                    if (marked == DirectoryType.TS) scanTestSet(dirPath, parent, indicator, scanned);
                    else scanTestSetPackage(dirPath, parent, indicator, unread, scanned);
                },
                () -> skipped(dirPath, DirectoryType.UNDER_TEST_CASES, unread));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-008, Rule-INTERNAL-015
    private void scanTestSetPackage(final @NotNull Path path, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread, final @NotNull ScannedProject scanned) {
        try {
            final @NotNull TestSetPackageDirectoryDto tsp = directoryMapper.getTestSetPackageNode(path, parent);

            scanned.getTestSetPackages().put(path.toString(), tsp);

            try (Stream<Path> subPaths = Files.list(path)) {
                subPaths.filter(Files::isDirectory)
                        .forEach(subPath -> scanTestSetOrPackage(subPath, tsp, indicator, unread, scanned));
            }

        } catch (final IOException | UncheckedIOException ex) {
            Logger.error("Failed to scan test set package: " + path.getFileName() + ": " + FailureText.of(ex));
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-011
    private void scanTestSet(final @NotNull Path path, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull ScannedProject scanned) {
        try {
            final @NotNull TestSetDirectoryDto ts = directoryMapper.getTestSetNode(path, parent);

            scanned.getTestSets().put(path.toString(), ts);

            final @NotNull List<UUID> testCaseIds = TestCaseSequenceStore.testCaseIds(List.of());

            try (Stream<Path> files = Files.list(path)) {
                files.filter(Files::isRegularFile)
                        .filter(file -> FileKind.of(file) == FileKind.TEST_CASE)
                        .parallel()
                        .forEach(filePath -> {
                            try {
                                final @NotNull TestCaseDto tc = mapper.readValue(Files.readAllBytes(filePath), TestCaseDto.class);
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
                            } catch (final IOException | UncheckedIOException ex) {
                                Logger.error("Failed to read test case '" + filePath.toAbsolutePath() +
                                        "': " + FailureText.of(ex));

                                scanned.getUnreadableTestCases().computeIfAbsent(path.toString(), _ -> ConcurrentHashMap.newKeySet())
                                        .add(filePath.getFileName().toString());
                            }
                        });
            }

            scanned.getTestCaseIdsByTestSet().put(path.toString(), testCaseIds);

            indicator.setText(Bundle.message("indexer.progress.test.set", ts.getName(), String.valueOf(testCaseIds.size())));

        } catch (final IOException | UncheckedIOException ex) {
            Logger.error("Failed to scan test set '" +
                    path.getFileName().toString() + "': " + FailureText.of(ex));
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-010, Rule-INTERNAL-015
    private void scanTestRunDirs(final @NotNull Path trDir, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread, final @NotNull ScannedProject scanned) {
        try (Stream<Path> paths = Files.list(trDir)) {
            final @NotNull List<Path> dirs = paths.filter(Files::isDirectory).toList();

            for (final Path dirPath : dirs) {
                if (indicator.isCanceled()) return;

                scanTestRunOrPackage(dirPath, parent, indicator, unread, scanned);
            }
        } catch (final NoSuchFileException absent) {
            Logger.info("No test runs to read: " + trDir);
        } catch (final IOException ex) {
            throw new UncheckedIOException("the folder " + trDir.getFileName() + " could not be listed: " + FailureText.of(ex), ex);
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-010, Rule-INTERNAL-015
    private void scanTestRunOrPackage(final @NotNull Path dirPath, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread, final @NotNull ScannedProject scanned) {
        store.markedAs(dirPath, DirectoryType.UNDER_TEST_RUNS).ifPresentOrElse(
                marked -> {
                    if (marked == DirectoryType.TR) scanTestRun(dirPath, parent, indicator, scanned);
                    else scanTestRunPackageDir(dirPath, parent, indicator, unread, scanned);
                },
                () -> skipped(dirPath, DirectoryType.UNDER_TEST_RUNS, unread));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-010, Rule-INTERNAL-015
    private void scanTestRunPackageDir(final @NotNull Path path, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread, final @NotNull ScannedProject scanned) {
        try {
            final @NotNull TestRunPackageDirectoryDto trp = directoryMapper.getTestRunPackageNode(path, parent);

            scanned.getTestRunPackages().put(path.toString(), trp);

            try (Stream<Path> subPaths = Files.list(path)) {
                subPaths.filter(Files::isDirectory)
                        .forEach(subPath -> scanTestRunOrPackage(subPath, trp, indicator, unread, scanned));
            }

        } catch (final IOException | UncheckedIOException ex) {
            Logger.error("Failed to scan test run package: " + path.getFileName() + ": " + FailureText.of(ex));
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
        } catch (final IOException | UncheckedIOException unreadable) {
            return false;
        }
    }

    // UC-INTERNAL-002
    private void scanTestRun(final @NotNull Path path, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull ScannedProject scanned) {
        final @NotNull TestRunDirectoryDto tr = directoryMapper.getTestRunNode(path, parent);

        scanned.getTestRunDirs().put(path.toString(), tr);

        // Rule-INTERNAL-011
        scanned.getTestRuns().put(path.toString(), new TestRunDto().setResults(resultsIn(path, scanned)));

        indicator.setText(Bundle.message("indexer.progress.test.run", path.getFileName()));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-011, Rule-INTERNAL-012
    private @NotNull List<TestRunItems> resultsIn(final @NotNull Path testRunPath, final @NotNull ScannedProject scanned) {
        final @NotNull List<TestRunItems> read = new ArrayList<>();

        for (final Path file : testDataFiles.resultsIn(testRunPath)) {
            // Rule-INTERNAL-094
            final @NotNull Optional<UUID> id = FileKind.RUN_ITEM.idIn(file);
            if (id.isEmpty()) {
                scanned.getHandNamedResults().add(testRunPath.getFileName() + "/" + file.getFileName());
                continue;
            }

            try {
                final @NotNull TestRunItems item = mapper.readValue(Files.readAllBytes(file), TestRunItems.class);
                item.setId(id.orElseThrow());
                read.add(item);

            } catch (final IOException | UncheckedIOException ex) {
                Logger.error("Failed to read the result '" + file.toAbsolutePath() + "': " + FailureText.of(ex));
                scanned.getUnreadableResults().add(testRunPath.getFileName() + "/" + file.getFileName());
            }
        }

        return inTestCaseOrder(read, scanned);
    }
}
