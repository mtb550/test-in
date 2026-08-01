package org.testin.util.indexer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.TestRunDto;
import org.testin.pojo.dto.dirs.*;
import org.testin.pojo.markers.TestRunMarker;
import org.testin.settings.Setting;
import org.testin.util.EditorUtil;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service(Service.Level.PROJECT)
public final class ProjectIndexer {

    private final @NotNull Project project;
    private final @NotNull IndexerDataStore store;
    private final @NotNull IndexingScanner scanner;
    private final @NotNull AtomicBoolean indexed = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean indexing = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean restoreEditorsOnComplete = new AtomicBoolean(true);

    private volatile @NotNull CountDownLatch indexingLatch = new CountDownLatch(1);

    public ProjectIndexer(final @NotNull Project project) {
        this.project = project;
        this.store = new IndexerDataStore(project);
        this.scanner = new IndexingScanner(project, store);
    }

    // todo: why static
    private static long estimateBytes(final int testCases, final int testRuns, final int projects, final int testSets, final int testRunDirs, final int testSetPkgs, final int testRunPkgs, final int testSetCaseSets) {
        final long MAP_OVERHEAD = 256L;
        final long TC_SIZE = 2048L;
        final long TR_SIZE = 1024L;
        final long DIR_SIZE = 512L;

        long total = 0;
        total += testCases * (TC_SIZE + MAP_OVERHEAD);
        total += testRuns * (TR_SIZE + MAP_OVERHEAD);
        total += projects * (DIR_SIZE + MAP_OVERHEAD);
        total += testSets * (DIR_SIZE + MAP_OVERHEAD);
        total += testRunDirs * (DIR_SIZE + MAP_OVERHEAD);
        total += testSetPkgs * (DIR_SIZE + MAP_OVERHEAD);
        total += testRunPkgs * (DIR_SIZE + MAP_OVERHEAD);
        total += testSetCaseSets * MAP_OVERHEAD;
        return total;
    }

    // todo: why static
    private static String formatBytes(final long bytes) {
        final long kb = bytes / 1024;
        if (kb < 1024) {
            return "~" + kb + " KB";
        }
        return String.format("~%.1f MB", kb / 1024.0);
    }

    public void indexWithProgress() {
        try {
            if (indexed.get() || indexing.getAndSet(true)) {
                return;
            }

            final Path rootPath = Services.getInstance(project, Setting.class).getTestinPath();
            if (rootPath.toString().isEmpty()) {
                indexing.set(false);
                indexed.set(true);
                indexingLatch.countDown();
                return;
            }

            final Path absoluteRoot = rootPath.isAbsolute()
                    ? rootPath
                    : (project.getBasePath() != null
                    ? Path.of(project.getBasePath(), rootPath.toString())
                    : rootPath);

            final List<Path> validProjects = collectValidProjects(absoluteRoot);
            if (validProjects.isEmpty()) {
                indexing.set(false);
                indexed.set(true);
                indexingLatch.countDown();
                Logger.warn("No valid projects found at '" + absoluteRoot.toAbsolutePath() + "'");
                return;
            }

            indexingLatch = new CountDownLatch(validProjects.size());
            Logger.info("Indexing " + validProjects.size() + " projects...");

            for (final Path projectPath : validProjects) {
                final String projectName = projectPath.getFileName().toString();

                ProgressManager.getInstance()
                        .run(new Task.Backgroundable(project, "Testin indexing - " + projectName, true) {
                            @Override
                            public void run(@NotNull ProgressIndicator indicator) {
                                indicator.setIndeterminate(false);
                                indicator.setFraction(0.0);
                                indicator.setText("Indexing " + projectName + "...");

                                try {
                                    scanner.scanProject(projectPath, indicator);
                                } catch (final Exception ex) {
                                    Logger.error("Failed to index project: " + projectName + " - " + ex.getMessage());
                                }

                                indicator.setFraction(1.0);
                                indicator.setText("Done - " + projectName);
                            }

                            @Override
                            public void onSuccess() {
                                indexingLatch.countDown();
                                Logger.info("Project '" + projectName + "' indexed.");

                                if (indexingLatch.getCount() == 0 && indexed.compareAndSet(false, true)) {
                                    indexing.set(false);

                                    logSummary();

                                    ApplicationManager.getApplication().invokeLater(() -> {
                                        if (restoreEditorsOnComplete.compareAndSet(true, true)) {
                                            Logger.info("Indexing finished, restoring open editors.");
                                            Services.getInstance(project, EditorUtil.class).restoreLastOpened(project);

                                        } else
                                            Logger.info("Indexing finished, skipping editor restore.");
                                    });
                                }
                            }

                            @Override
                            public void onThrowable(@NotNull Throwable error) {
                                indexingLatch.countDown();
                                Logger.error("Error indexing '" + projectName + "': " + error.getMessage());
                            }
                        });
            }
        } catch (final Exception ex) {
            Logger.error("indexWithProgress: " + ex.getMessage());
            indexing.set(false);
        }
    }

    // todo: to be removed
    public boolean isIndexed() {
        return indexed.get();
    }

    public void awaitIndexing() {
        if (indexed.get()) return;
        try {
            indexingLatch.await();
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public void dispose() {
        store.clearAll();
        indexed.set(false);
        indexing.set(false);
        indexingLatch = new CountDownLatch(1);
        Logger.info("Indexer disposed");
    }

    public void resetForReindex() {
        restoreEditorsOnComplete.set(false);
        store.clearAll();
        indexed.set(false);
        indexing.set(false);
        indexingLatch = new CountDownLatch(1);
        Logger.info("Indexer reset for re-indexing");
    }

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

        return Arrays.stream(projectPaths)
                .filter(p -> Files.exists(p.resolve(DirectoryType.TP.getMarker())))
                .collect(Collectors.toList());
    }

    private void logSummary() {
        final int tcCount = store.getTestCasesById().size();
        final int trCount = store.getTestRunsByPath().size();
        final int projCount = store.getTestProjectsByPath().size();
        final int setCount = store.getTestSetsDirByPath().size();
        final int runDirCount = store.getTestRunsDirByPath().size();
        final int setPkgCount = store.getTestSetPackagesByPath().size();
        final int runPkgCount = store.getTestRunPackagesByPath().size();

        final long estimatedBytes = estimateBytes(
                tcCount, trCount, projCount, setCount, runDirCount,
                setPkgCount, runPkgCount, store.getTestSetCaseIds().size());

        Logger.info("Indexing complete: " +
                tcCount + " test cases, " +
                trCount + " test runs, " +
                projCount + " projects, " +
                setCount + " test sets, " +
                runDirCount + " test run dirs, " +
                setPkgCount + " test set packages, " +
                runPkgCount + " test run packages | " +
                formatBytes(estimatedBytes));
    }

    public @NotNull List<TestCaseDto> getTestCasesForTestSet(final @NotNull Path testSetPath) {
        return store.getTestCasesForTestSet(testSetPath);
    }

    public @NotNull TestRunDto getTestRunByPath(final @NotNull Path testRunPath) {
        return store.getTestRunByPath(testRunPath);
    }

    public @NotNull TestCaseDto getTestCaseById(final @NotNull UUID id) {
        return store.getTestCaseById(id);
    }

    public @NotNull TestSetDirectoryDto getTestSetByPath(final @NotNull Path path) {
        return store.getTestSetDirByPath(path);
    }

    public @NotNull TestSetPackageDirectoryDto getTestSetPackageByPath(final @NotNull Path path) {
        return store.getTestSetPackageByPath(path);
    }

    public @NotNull TestCasesMainDirectoryDto getTestCasesMainDirByPath(final @NotNull Path path) {
        return store.getTestCasesMainDirByPath(path);
    }

    public @NotNull TestRunsMainDirectoryDto getTestRunsMainDirByPath(final @NotNull Path path) {
        return store.getTestRunsMainDirByPath(path);
    }

    public @NotNull TestRunDirectoryDto getTestRunDirByPath(final @NotNull Path path) {
        return store.getTestRunDirByPath(path);
    }

    public @NotNull Map<String, TestProjectDirectoryDto> getTestProjectsByPath() {
        return store.getTestProjectsByPath();
    }

    public @NotNull List<DirectoryDto> getChildren(final @NotNull Path parentPath) {
        return store.getChildren(parentPath);
    }

    public void putTestCase(final @NotNull Path testSetPath, final @NotNull TestCaseDto tc) {
        store.putTestCase(testSetPath, tc);
    }

    public void removeTestCase(final @NotNull Path testSetPath, final @NotNull UUID tcId) {
        store.removeTestCase(testSetPath, tcId);
    }

    public void updateSequence(final @NotNull Path testSetPath, final @NotNull List<TestCaseDto> sortedList) {
        store.updateSequence(testSetPath, sortedList);
    }

    public void putTestRun(final @NotNull Path testRunPath, final @NotNull TestRunDto tr) {
        store.putTestRun(testRunPath, tr);
    }

    public void removeTestProject(final @NotNull Path path) {
        store.removeTestProject(path);
    }

    public void removeTestSet(final @NotNull Path path) {
        store.removeTestSet(path);
    }

    public void removeTestRun(final @NotNull Path path) {
        store.removeTestRun(path);
    }

    public void removeTestSetPackage(final @NotNull Path path) {
        store.removeTestSetPackage(path);
    }

    public void removeTestRunPackage(final @NotNull Path path) {
        store.removeTestRunPackage(path);
    }

    public void addTestProject(final @NotNull TestProjectDirectoryDto tp) {
        store.addTestProject(tp);
        store.addTestProjectMarker(project, tp);
    }

    public void addTestSet(final @NotNull TestSetDirectoryDto ts) {
        store.addTestSet(ts);
    }

    public void addTestSetPackage(final @NotNull TestSetPackageDirectoryDto tsp) {
        store.addTestSetPackage(tsp);
    }

    public void addTestRunDir(final @NotNull TestRunDirectoryDto trd) {
        store.addTestRunDir(trd);
    }

    public void addTestRunPackage(final @NotNull TestRunPackageDirectoryDto trp) {
        store.addTestRunPackage(trp);
    }

    public void scanSingleProject(final @NotNull Path projectPath) {
        Logger.info("Scanning single project: " + projectPath.getFileName());
        try {
            scanner.scanProject(projectPath);
        } catch (final Exception ex) {
            Logger.error("Failed to scan single project: " + ex.getMessage());
        }
    }

    public void persistTestProjectMarker(final @NotNull Project project, final @NotNull TestProjectDirectoryDto tp) {
        store.addTestProjectMarker(project, tp);
    }

    public void updateRunMarker(final @NotNull Project project, final @NotNull Path runPath, final @NotNull TestRunMarker marker) {
        store.updateRunMarker(project, runPath, marker);
    }

    public void renameNode(final @NotNull Path oldPath, final @NotNull Path newPath) {
        store.renameNode(oldPath, newPath);
    }
}
