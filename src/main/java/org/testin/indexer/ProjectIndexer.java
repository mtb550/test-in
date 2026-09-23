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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.LastOpenEditors;
import org.testin.logger.Logger;
import org.testin.model.DirectoryType;
import org.testin.model.FileKind;
import org.testin.model.ProjectStatus;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.dto.dirs.TestRunPackageDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.model.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.model.markers.AbstractMarker;
import org.testin.model.markers.TestRunMarker;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.setting.TestinRoot;
import org.testin.testproject.BoundTestProject;
import org.testin.util.Bundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service(Service.Level.PROJECT)
public final class ProjectIndexer {
    private final @NotNull Project p;
    private final @NotNull IndexerDataStore store;
    private final @NotNull ProjectScanCoordinator scanCoordinator;
    private final @NotNull AtomicBoolean indexed = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean indexing = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean restoreEditorsOnComplete = new AtomicBoolean(true);
    private final @NotNull RunWriter runWriter;
    private final @NotNull NodeFiles nodeFiles;
    private volatile @NotNull CountDownLatch indexingLatch = new CountDownLatch(1);

    public ProjectIndexer(final @NotNull Project p) {
        this.p = p;
        this.store = new IndexerDataStore(p);
        this.scanCoordinator = new ProjectScanCoordinator(new IndexingScanner(p, store));
        this.runWriter = new RunWriter(p, store);
        this.nodeFiles = new NodeFiles(p, this, store);
    }

    private static boolean sameFile(final @NotNull Path one, final @NotNull Path other) {
        try {
            return Files.isSameFile(one, other);
        } catch (final IOException ex) {
            Logger.warn("Could not compare " + one + " with " + other + ": " + ex.getMessage());
            return false;
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-011
    static boolean isTestCaseFile(final @NotNull Path file, final @NotNull Predicate<Path> isTestSet) {
        return FileKind.of(file) == FileKind.TEST_CASE && isTestSet.test(file.getParent());
    }

    // UC-INTERNAL-002, Rule-INTERNAL-013
    public void indexWithProgress() {
        try {
            if (indexed.get() || indexing.getAndSet(true)) {
                return;
            }

            final @NotNull Path absoluteRoot = absoluteRoot();
            if (absoluteRoot.toString().isEmpty()) {
                indexing.set(false);
                indexed.set(true);
                indexingLatch.countDown();
                return;
            }

            final @NotNull List<Path> validProjects = boundOnly(collectValidProjects(absoluteRoot));
            if (validProjects.isEmpty()) {
                indexing.set(false);
                indexed.set(true);
                indexingLatch.countDown();
                Logger.warn("No valid projects found at '" + absoluteRoot.toAbsolutePath() + "'");
                return;
            }

            final @NotNull AtomicInteger projectsLeft = new AtomicInteger(validProjects.size());
            final @NotNull CountDownLatch passLatch = indexingLatch;
            Logger.info("Indexing " + validProjects.size() + " projects..");

            for (final Path projectPath : validProjects) {
                final @NotNull String projectName = projectPath.getFileName().toString();

                ProgressManager.getInstance()
                        .run(new Task.Backgroundable(p, Bundle.message("indexer.task.title", projectName), true) {
                            @Override
                            public void run(final @NotNull ProgressIndicator indicator) {
                                indicator.setIndeterminate(false);
                                indicator.setFraction(0.0);
                                indicator.setText(Bundle.message("indexer.progress.indexing", projectName));

                                try {
                                    scanCoordinator.scan(projectPath, indicator);
                                } catch (final Exception ex) {
                                    Logger.error("Failed to index project: " + projectName + " - " + ex.getMessage());
                                }

                                indicator.setFraction(1.0);
                                indicator.setText(Bundle.message("indexer.progress.done", projectName));
                            }

                            @Override
                            public void onSuccess() {
                                Logger.info("Project '" + projectName + "' indexed.");
                                if (oneProjectFinished(projectsLeft, passLatch)) finishSuccessfully();
                            }

                            @Override
                            public void onThrowable(final @NotNull Throwable error) {
                                Logger.error("Error indexing '" + projectName + "': " + error.getMessage());
                                if (oneProjectFinished(projectsLeft, passLatch)) finishWithFailure();
                            }
                        });
            }
        } catch (final Exception ex) {
            Logger.error("indexWithProgress: " + ex.getMessage());
            indexing.set(false);

            indexingLatch.countDown();
        }
    }

    private boolean oneProjectFinished(final @NotNull AtomicInteger projectsLeft, final @NotNull CountDownLatch passLatch) {
        if (projectsLeft.decrementAndGet() != 0) return false;

        passLatch.countDown();
        return passLatch == indexingLatch;
    }

    private void finishSuccessfully() {
        if (!indexed.compareAndSet(false, true)) return;

        indexing.set(false);
        logSummary();
        restoreOpenEditorsOnce();
    }

    // UC-INTERNAL-002
    private void finishWithFailure() {
        indexing.set(false);
        Logger.warn("Indexing finished with errors; will retry on the next request.");
    }

    private void restoreOpenEditorsOnce() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (restoreEditorsOnComplete.getAndSet(false)) {
                Logger.info("Indexing finished, restoring open editors.");
                Services.getInstance(p, LastOpenEditors.class).reopen(p);
            } else {
                Logger.info("Indexing finished, skipping editor restore.");
            }
        });
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-118
    public boolean isIndexed() {
        return indexed.get();
    }

    public void awaitIndexing() {
        if (indexed.get()) return;

        if (ApplicationManager.getApplication().isReadAccessAllowed()) {
            Logger.error("awaitIndexing() called while holding a read action - "
                    + "this blocks every write action in the IDE. Returning without waiting.");
            return;
        }

        try {
            indexingLatch.await();
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public void resetForReindex() {
        scanCoordinator.exclusively(() -> {
            restoreEditorsOnComplete.set(false);
            store.clearAll();
            indexed.set(false);
            indexing.set(false);
            indexingLatch = new CountDownLatch(1);
            Logger.info("Indexer reset for re-indexing");
        });
    }

    private @NotNull Path absoluteRoot() {
        return Services.getInstance(p, TestinRoot.class).absolutePath();
    }

    // UC-INTERNAL-002, Rule-INTERNAL-006
    private @NotNull List<Path> boundOnly(final @NotNull List<Path> projects) {
        final @NotNull String bound = Services.getInstance(p, BoundTestProject.class).name();
        if (bound.isEmpty()) return projects;

        final @NotNull List<Path> scoped = projects.stream()
                .filter(this::isBound)
                .toList();

        if (scoped.isEmpty()) {
            Logger.warn("This repository's test project '" + bound + "' is not a test project under the root");
            return scoped;
        }

        Logger.info("Indexing only the bound project '" + bound + "'");
        return scoped;
    }

    private boolean isBound(final @NotNull Path projectPath) {
        final @NotNull String bound = Services.getInstance(p, BoundTestProject.class).name();
        return bound.isEmpty() || bound.equals(projectPath.getFileName().toString());
    }

    public @NotNull Map<String, ProjectStatus> testProjects() {
        final @NotNull Map<String, ProjectStatus> byName = new LinkedHashMap<>();
        final @NotNull Path root = absoluteRoot();
        if (root.toString().isEmpty()) return byName;

        for (final Path path : collectValidProjects(root)) {
            final @NotNull String name = path.getFileName().toString();
            try {
                byName.put(name, Services.getInstance(p, DirectoryMapper.class)
                        .getTestProjectNode(p, path).getMarker().getStatus());

            } catch (final Exception ex) {
                Logger.warn("Could not read test project '" + name + "': " + ex.getMessage());
            }
        }

        return byName;
    }

    // UC-INTERNAL-002, Rule-INTERNAL-003, Rule-INTERNAL-004
    private @NotNull List<Path> collectValidProjects(final @NotNull Path rootPath) {
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) return Collections.emptyList();

        final Path[] projectPaths;
        try (Stream<Path> dirs = Files.list(rootPath)) {
            projectPaths = dirs.filter(Files::isDirectory).toArray(Path[]::new);
        } catch (final Exception ex) {
            Logger.error("Failed to list root directory: " + ex.getMessage());
            return Collections.emptyList();
        }

        if (projectPaths.length == 0) return Collections.emptyList();

        final @NotNull List<Path> valid = new ArrayList<>();
        Arrays.stream(projectPaths).forEach(folder -> {
            if (isTestProjectFolder(folder)) {
                valid.add(folder);
            } else {
                Logger.warn("Skipping directory without a " + DirectoryType.TP.getMarker()
                        + " marker (not a test project): " + folder);
            }
        });
        return valid;
    }

    private boolean isTestProjectFolder(final @NotNull Path folder) {
        return store.hasMarker(folder, DirectoryType.TP);
    }

    private void logSummary() {
        Logger.info("Indexing complete: " +
                store.getTestCasesById().size() + " test cases, " +
                store.getTestRunsByPath().size() + " test runs, " +
                store.getTestProjectsByPath().size() + " projects, " +
                store.getTestSetsDirByPath().size() + " test sets, " +
                store.getTestRunsDirByPath().size() + " test run dirs, " +
                store.getTestSetPackagesByPath().size() + " test set packages, " +
                store.getTestRunPackagesByPath().size() + " test run packages");
    }

    public @NotNull List<TestCaseDto> getTestCasesForTestSet(final @NotNull Path testSetPath) {
        return store.getTestCasesForTestSet(testSetPath);
    }

    // UC-INTERNAL-006, Rule-INTERNAL-046
    public long testCaseCountOf(final @NotNull Path testSetPath) {
        return store.getTestCaseIdsByTestSet().getOrDefault(testSetPath.toString(), List.of()).size();
    }

    // Rule-TREE-PANEL-008
    public @NotNull List<TestCaseDto> getTestCasesUnder(final @NotNull DirectoryDto dir) {
        final @NotNull List<TestCaseDto> testCases = new ArrayList<>(getTestCasesForTestSet(dir.getPath()));

        for (final DirectoryDto child : getChildren(dir.getPath())) {
            if (child.isRetired()) continue;

            testCases.addAll(getTestCasesUnder(child));
        }

        return testCases;
    }

    public @NotNull TestRunDto getTestRunByPath(final @NotNull Path testRunPath) {
        return withTestCasesShown(store.getTestRunByPath(testRunPath));
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126, Rule-REPORT-021, Rule-VIEW-PANEL-083
    private @NotNull TestRunDto withTestCasesShown(final @NotNull TestRunDto run) {
        run.getResults().forEach(item -> item.showing(store.findTestCase(item.getId())));
        return run;
    }

    // UC-VIEW-PANEL-008, Rule-VIEW-PANEL-065
    public @NotNull Map<Path, TestRunDto> getAllTestRuns() {
        return store.getTestRunsByPath().entrySet().stream()
                .collect(Collectors.toMap(entry -> Path.of(entry.getKey()), entry -> withTestCasesShown(entry.getValue())));
    }

    // UC-INTERNAL-006, Rule-INTERNAL-051
    public @NotNull Optional<TestRunDto> findTestRun(final @NotNull Path testRunPath) {
        return store.findTestRun(testRunPath).map(this::withTestCasesShown);
    }

    public @NotNull Optional<TestCaseDto> findTestCase(final @NotNull UUID id) {
        return store.findTestCase(id);
    }

    public @NotNull TestSetDirectoryDto getTestSetByPath(final @NotNull Path path) {
        return store.getTestSetDirByPath(path);
    }

    public @NotNull Map<String, TestProjectDirectoryDto> getTestProjectsByPath() {
        return store.getTestProjectsByPath();
    }

    // UC-TREE-PANEL-002, UC-TREE-PANEL-003, UC-TREE-PANEL-011, Rule-TREE-PANEL-004
    public boolean isTaken(final @NotNull Path wanted, final @NotNull Optional<Path> renaming) {
        if (!Files.exists(wanted)) return false;

        return renaming.map(self -> !sameFile(self, wanted)).orElse(true);
    }

    public @NotNull List<DirectoryDto> getChildren(final @NotNull Path parentPath) {
        return store.getChildren(parentPath);
    }

    // UC-INTERNAL-004, Rule-INTERNAL-033
    public boolean putTestCase(final @NotNull Path testSetPath, final @NotNull TestCaseDto tc) {
        return store.putTestCase(testSetPath, tc);
    }

    // UC-INTERNAL-004, Rule-INTERNAL-035
    public boolean putTestCaseVerbatim(final @NotNull Path testSetPath, final @NotNull TestCaseDto tc) {
        return store.putTestCaseVerbatim(testSetPath, tc);
    }

    // UC-EDITOR-PANEL-017, Rule-INTERNAL-035
    public boolean moveTestCase(final @NotNull Path fromSet, final @NotNull Path toSet, final @NotNull TestCaseDto tc) {
        return store.moveTestCase(fromSet, toSet, tc);
    }

    // UC-EDITOR-PANEL-011, Rule-EDITOR-PANEL-064
    public boolean removeTestCase(final @NotNull Path testSetPath, final @NotNull UUID tcId) {
        if (!store.removeTestCase(testSetPath, tcId)) return false;

        Services.getInstance(p, TestCaseValues.class).reload(this::getAllTestCases);
        return true;
    }

    public @NotNull List<TestCaseDto> getAllTestCases() {
        return List.copyOf(store.getTestCasesById().values());
    }

    public @NotNull List<DirectoryDto> getAllNodes() {
        return List.copyOf(store.allDirectories());
    }

    // UC-INTERNAL-004, Rule-INTERNAL-031
    public void updateSequence(final @NotNull Path testSetPath, final @NotNull List<TestCaseDto> orderedList, final @NotNull List<TestCaseDto> moved) {
        store.updateSequence(testSetPath, orderedList, moved);
    }

    public void changeRun(final @NotNull Path runPath, final @NotNull Consumer<TestRunDto> change) {
        findTestRun(runPath).ifPresentOrElse(run -> {
            // Rule-INTERNAL-011
            final @NotNull Set<UUID> gone = run.coveredIds();
            change.accept(run);
            gone.removeAll(run.coveredIds());

            runWriter.persist(runPath, run, gone);
        }, () -> Logger.warn("Test run no longer indexed, so a change to it was dropped: " + runPath.getFileName()));
    }

    public void changeRunMarker(final @NotNull Path runPath, final @NotNull Consumer<TestRunMarker> change) {
        store.findTestRunDir(runPath).ifPresentOrElse(dir -> {
            final @NotNull TestRunMarker marker = dir.getMarker();
            change.accept(marker);
            runWriter.persistMarker(runPath);
        }, () -> Logger.warn("Test run no longer indexed, so a change to its marker was dropped: " + runPath.getFileName()));
    }

    public void saveRun(final @NotNull Path runPath) {
        changeRun(runPath, _ -> {
        });
    }

    public void putTestRun(final @NotNull Path testRunPath, final @NotNull TestRunDto tr) {
        runWriter.create(testRunPath, tr);
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    public @NotNull List<String> storeScreenshots(final @NotNull Path runPath, final @NotNull List<byte[]> pngs) {
        return runWriter.storeScreenshots(runPath, pngs);
    }

    public byte @NotNull [] screenshot(final @NotNull Path runPath, final @NotNull String name) {
        return runWriter.readScreenshot(runPath, name);
    }

    public @NotNull List<byte[]> screenshots(final @NotNull Path runPath, final @NotNull TestRunItems item) {
        return item.getScreenshots().stream().map(name -> screenshot(runPath, name)).toList();
    }

    // UC-INTERNAL-005
    public void removeTestProject(final @NotNull Path path, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        removeVf(path, () -> store.removeTestProject(path), onRemoved);
    }

    public void removeTestSet(final @NotNull Path path, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        removeVf(path, () -> store.removeTestSet(path), onRemoved);
    }

    public void removeTestRun(final @NotNull Path path, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        removeVf(path, () -> store.removeTestRun(path), onRemoved);
    }

    public void removeTestSetPackage(final @NotNull Path path, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        removeVf(path, () -> store.removeTestSetPackage(path), onRemoved);
    }

    public void removeTestRunPackage(final @NotNull Path path, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        removeVf(path, () -> store.removeTestRunPackage(path), onRemoved);
    }

    // UC-TREE-PANEL-012, Rule-TREE-PANEL-042
    public void refuseRemove(final @NotNull Path path, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        Logger.info("Not removed: " + path.getFileName() + " is not removable from the tree");
        onRemoved.accept(false);
    }

    private void removeVf(final @NotNull Path path, final @NotNull Runnable cacheUpdate, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        nodeFiles.remove(path, cacheUpdate, onRemoved);
    }

    public void moveNode(final @NotNull Path oldPath, final @NotNull Path newPath, final @NotNull Consumer<@NotNull Boolean> onFinished) {
        nodeFiles.move(oldPath, newPath, onFinished);
    }

    public void copyNodes(final @NotNull List<Path> sourcePaths, final @NotNull Path targetPath, final @NotNull IntConsumer onComplete) {
        nodeFiles.copy(sourcePaths, targetPath, onComplete);
    }

    // UC-INTERNAL-005, Rule-INTERNAL-037, Rule-INTERNAL-041
    public @NotNull Optional<Path> keepAside(final @NotNull Path node) {
        return Services.getInstance(DeletedNodes.class).keep(node);
    }

    // UC-INTERNAL-005, Rule-INTERNAL-042
    public boolean restoreNode(final @NotNull Path kept, final @NotNull Path original) {
        if (!Services.getInstance(DeletedNodes.class).putBack(p, kept, original)) return false;

        refreshDirectory(original);
        refreshIndexedProject(original);
        return true;
    }

    // UC-INTERNAL-005, Rule-INTERNAL-043
    public void forgetKept(final @NotNull Path kept) {
        Services.getInstance(DeletedNodes.class).forget(kept);
    }

    void refreshIndexedProject(final @NotNull Path changedPath) {
        testProjectHolding(changedPath).ifPresent(scanCoordinator::rescanExclusively);
    }

    private @NotNull Optional<Path> testProjectHolding(final @NotNull Path path) {
        return store.getTestProjectsByPath().keySet().stream()
                .map(Path::of)
                .filter(path::startsWith)
                .max(Comparator.comparingInt(Path::getNameCount));
    }

    // UC-SHARE-002, Rule-SHARE-001
    public @NotNull Set<String> unreadableTestCasesIn(final @NotNull Path testSetPath) {
        return store.unreadableTestCasesIn(testSetPath);
    }

    // UC-INTERNAL-004, Rule-INTERNAL-034
    public @NotNull Optional<TestCaseFile> testCaseFile(final @NotNull TestCaseDto tc) {
        final @NotNull Path file = store.testCaseFileOf(tc);
        return testProjectHolding(file).map(testProject -> new TestCaseFile(testProject, testProject.relativize(file)));
    }

    // UC-TREE-PANEL-002
    public boolean addTestProject(final @NotNull TestProjectDirectoryDto tp) {
        return store.addTestProject(tp);
    }

    public boolean addTestSet(final @NotNull TestSetDirectoryDto ts) {
        return store.addTestSet(ts);
    }

    public boolean addTestSetPackage(final @NotNull TestSetPackageDirectoryDto tsp) {
        return store.addTestSetPackage(tsp);
    }

    public boolean addTestRunDir(final @NotNull TestRunDirectoryDto trd) {
        return store.addTestRunDir(trd);
    }

    public boolean addTestRunPackage(final @NotNull TestRunPackageDirectoryDto trp) {
        return store.addTestRunPackage(trp);
    }

    // UC-INTERNAL-003, Rule-INTERNAL-016
    public void rescanChangedProject(final @NotNull Path projectPath, final @NotNull ProgressIndicator indicator) {
        if (isTestProjectFolder(projectPath) && isBound(projectPath)) {
            scanSingleProject(projectPath, indicator);
            return;
        }

        Logger.info("Not a test project Testin reads, so not scanned: " + projectPath);
        store.removeTestProject(projectPath);
        store.invalidateChildrenIndex();
    }

    public void scanSingleProject(final @NotNull Path projectPath) {
        scanSingleProject(projectPath, new EmptyProgressIndicator());
    }

    // UC-INTERNAL-003, Rule-INTERNAL-021
    public void scanSingleProject(final @NotNull Path projectPath, final @NotNull ProgressIndicator indicator) {
        Logger.info("Scanning single project: " + projectPath.getFileName());
        try {
            scanCoordinator.scan(projectPath, indicator);
        } catch (final Exception ex) {
            Logger.error("Failed to scan single project: " + ex.getMessage());
        }
    }

    public boolean persistMarker(final @NotNull DirectoryDto dto) {
        return store.persistMarker(dto);
    }

    public <M extends AbstractMarker> @NotNull M readMarker(final @NotNull Path dirPath, final @NotNull DirectoryType kind, final @NotNull String name, final @NotNull Class<M> markerClass) {
        return store.readMarker(dirPath, kind, name, markerClass);
    }

    // UC-INTERNAL-008, Rule-INTERNAL-091
    public void convertEveryProject() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (p.isDisposed()) return;

            Services.getInstance(Conversions.class).sweep(p);
        });
    }

    // Rule-INTERNAL-091
    public @NotNull Optional<String> whyNotRead(final @NotNull Path projectPath) {
        return store.whyNotRead(projectPath);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-014
    public @NotNull List<String> takeDamagedMarkers() {
        return store.takeDamagedMarkers();
    }

    public @NotNull Optional<DirectoryDto> find(final @NotNull Path path) {
        return store.findByPath(path);
    }

    public boolean nodeExists(final @NotNull Path path) {
        return store.findByPath(path).isPresent();
    }

    public void refreshDirectory(final @NotNull Path path) {
        store.refreshDir(path);
    }

    public void renameNode(final @NotNull Path oldPath, final @NotNull Path newPath, final @NotNull Runnable onFinished) {
        nodeFiles.rename(oldPath, newPath, onFinished);
    }
}
