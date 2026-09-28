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
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.LastOpenEditors;
import org.testin.logger.Logger;
import org.testin.model.DirectoryType;
import org.testin.model.FileKind;
import org.testin.model.ProjectStatus;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.testproject.BoundTestProject;
import org.testin.util.Bundle;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Service(Service.Level.PROJECT)
public final class ProjectIndexer {
    private final @NotNull Project p;
    @Getter(AccessLevel.PACKAGE)
    private final @NotNull IndexerDataStore store;
    @Getter(AccessLevel.PACKAGE)
    private final @NotNull ProjectScanCoordinator scanCoordinator;
    private final @NotNull AtomicBoolean indexed = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean indexing = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean restoreEditorsOnComplete = new AtomicBoolean(true);
    @Getter(AccessLevel.PACKAGE)
    private final @NotNull RunWriter runWriter;
    @Getter(AccessLevel.PACKAGE)
    private final @NotNull NodeFiles nodeFiles;
    private volatile @NotNull CountDownLatch indexingLatch = new CountDownLatch(1);

    public ProjectIndexer(final @NotNull Project p) {
        this.p = p;
        this.store = new IndexerDataStore(p);
        this.scanCoordinator = new ProjectScanCoordinator(new IndexingScanner(p, store));
        this.runWriter = new RunWriter(p, store);
        this.nodeFiles = new NodeFiles(p, this, store);
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

                                final long started = System.nanoTime();
                                try {
                                    scanCoordinator.scan(projectPath, indicator);
                                } catch (final Exception ex) {
                                    Logger.error("Failed to index project: " + projectName + " - " + ex.getMessage());
                                }
                                Logger.info("First read of '" + projectName + "' took " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) + " ms");

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

    // UC-INTERNAL-002, Rule-INTERNAL-111, Rule-INTERNAL-115
    public void awaitIndexing() {
        if (indexed.get()) return;

        indexWithProgress();

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

    // UC-INTERNAL-002, Rule-INTERNAL-114
    void announce(final @NotNull Path changed) {
        if (p.isDisposed()) return;

        final @NotNull IndexChanged listeners = p.getMessageBus().syncPublisher(IndexChanged.TOPIC);
        if (absoluteRoot().equals(changed.getParent())) listeners.testProjectsChanged();
        else listeners.nodesChanged(Set.of(changed.getParent()));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-114
    boolean announcedIf(final boolean changed, final @NotNull Path path) {
        if (changed) announce(path);
        return changed;
    }

    // UC-INTERNAL-002, Rule-INTERNAL-114
    void announceChildrenOf(final @NotNull Path folder) {
        if (!p.isDisposed()) p.getMessageBus().syncPublisher(IndexChanged.TOPIC).nodesChanged(Set.of(folder));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-114
    private void announceReadAgain() {
        if (!p.isDisposed()) p.getMessageBus().syncPublisher(IndexChanged.TOPIC).readAgain();
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

    void refreshIndexedProject(final @NotNull Path changedPath) {
        testProjectHolding(changedPath).ifPresent(scanCoordinator::rescanExclusively);
    }

    @NotNull Optional<Path> testProjectHolding(final @NotNull Path path) {
        return store.getTestProjectsByPath().keySet().stream()
                .map(Path::of)
                .filter(path::startsWith)
                .max(Comparator.comparingInt(Path::getNameCount));
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
        announceReadAgain();
    }

    public void scanSingleProject(final @NotNull Path projectPath) {
        scanSingleProject(projectPath, new EmptyProgressIndicator());
    }

    // UC-INTERNAL-003, Rule-INTERNAL-021
    public void scanSingleProject(final @NotNull Path projectPath, final @NotNull ProgressIndicator indicator) {
        Logger.info("Scanning single project: " + projectPath.getFileName());
        Services.getInstance(Rescan.class).coveredByAScan(projectPath);
        try {
            scanCoordinator.scan(projectPath, indicator);
        } catch (final Exception ex) {
            Logger.error("Failed to scan single project: " + ex.getMessage());
        }

        announceReadAgain();
    }

    // Rule-INTERNAL-091
    public @NotNull Optional<String> whyNotRead(final @NotNull Path projectPath) {
        return store.whyNotRead(projectPath);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-014
    public @NotNull List<Path> takeDamagedMarkers() {
        return store.takeDamagedMarkers();
    }
}
