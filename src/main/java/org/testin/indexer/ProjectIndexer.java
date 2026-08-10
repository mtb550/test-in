package org.testin.indexer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.DirectoryType;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.TestRunDto;
import org.testin.mappers.dto.dirs.*;
import org.testin.mappers.markers.TestRunMarker;
import org.testin.services.Services;
import org.testin.settings.Setting;
import org.testin.util.EditorUtil;
import org.testin.util.TreeUtilImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service(Service.Level.PROJECT)
public final class ProjectIndexer {

    private final @NotNull Project p;
    private final @NotNull IndexerDataStore store;
    private final @NotNull ProjectScanCoordinator scanCoordinator;
    private final @NotNull AtomicBoolean indexed = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean indexing = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean restoreEditorsOnComplete = new AtomicBoolean(true);

    private volatile @NotNull CountDownLatch indexingLatch = new CountDownLatch(1);

    public ProjectIndexer(final @NotNull Project p) {
        this.p = p;
        this.store = new IndexerDataStore(p);
        this.scanCoordinator = new ProjectScanCoordinator(new IndexingScanner(p, store));
    }

    private long estimateBytes(final int testCases, final int testRuns, final int projects, final int testSets, final int testRunDirs, final int testSetPkgs, final int testRunPkgs, final int testSetCaseSets) {
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

    private String formatBytes(final long bytes) {
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

            final Path rootPath = Services.getInstance(p, Setting.class).getTestinPath();
            if (rootPath.toString().isEmpty()) {
                indexing.set(false);
                indexed.set(true);
                indexingLatch.countDown();
                return;
            }

            final Path absoluteRoot = rootPath.isAbsolute()
                    ? rootPath
                    : (p.getBasePath() != null
                    ? Path.of(p.getBasePath(), rootPath.toString())
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
                        .run(new Task.Backgroundable(p, "Testin indexing - " + projectName, true) {
                            @Override
                            public void run(@NotNull ProgressIndicator indicator) {
                                indicator.setIndeterminate(false);
                                indicator.setFraction(0.0);
                                indicator.setText("Indexing " + projectName + "...");

                                try {
                                    scanCoordinator.scan(projectPath, indicator);
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
                                finishSuccessfully();
                            }

                            @Override
                            public void onThrowable(@NotNull Throwable error) {
                                indexingLatch.countDown();
                                Logger.error("Error indexing '" + projectName + "': " + error.getMessage());
                                finishWithFailure();
                            }
                        });
            }
        } catch (final Exception ex) {
            Logger.error("indexWithProgress: " + ex.getMessage());
            indexing.set(false);
        }
    }

    private void finishSuccessfully() {
        if (indexingLatch.getCount() == 0 && indexed.compareAndSet(false, true)) {
            indexing.set(false);
            logSummary();
            restoreOpenEditorsOnce();
        }
    }

    private void finishWithFailure() {
        if (indexingLatch.getCount() == 0) {
            indexing.set(false);
            Logger.warn("Indexing finished with errors; will retry on the next request.");
        }
    }

    private void restoreOpenEditorsOnce() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (restoreEditorsOnComplete.getAndSet(false)) {
                Logger.info("Indexing finished, restoring open editors.");
                Services.getInstance(p, EditorUtil.class).restoreLastOpened(p);
            } else {
                Logger.info("Indexing finished, skipping editor restore.");
            }
        });
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

        final List<Path> valid = new ArrayList<>();
        Arrays.stream(projectPaths).forEach(p -> {
            if (Files.exists(p.resolve(DirectoryType.TP.getMarker()))) {
                valid.add(p);
            } else {
                Logger.warn("Skipping directory without .tp marker (not a test project): " + p);
            }
        });
        return valid;
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

    public @Nullable TestRunDto getTestRunByPath(final @NotNull Path testRunPath) {
        return store.getTestRunByPath(testRunPath);
    }

    public @Nullable TestCaseDto getTestCaseById(final @NotNull UUID id) {
        return store.getTestCaseById(id);
    }

    public @Nullable TestSetDirectoryDto getTestSetByPath(final @NotNull Path path) {
        return store.getTestSetDirByPath(path);
    }

    public @Nullable TestSetPackageDirectoryDto getTestSetPackageByPath(final @NotNull Path path) {
        return store.getTestSetPackageByPath(path);
    }

    public @Nullable TestRunDirectoryDto getTestRunDirByPath(final @NotNull Path path) {
        return store.getTestRunDirByPath(path);
    }

    public @NotNull Map<String, TestProjectDirectoryDto> getTestProjectsByPath() {
        return store.getTestProjectsByPath();
    }


    public boolean rootExists() {
        final Path root = Services.getInstance(p, Setting.class).getTestinPath();
        return Files.isDirectory(root);
    }

    public boolean projectExists(final @NotNull Path projectPath) {
        return Files.isDirectory(projectPath);
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
        removeVf(path);
        store.removeTestProject(path);
    }

    public void removeTestSet(final @NotNull Path path) {
        removeVf(path);
        store.removeTestSet(path);
    }

    public void removeTestRun(final @NotNull Path path) {
        removeVf(path);
        store.removeTestRun(path);
    }

    public void removeTestSetPackage(final @NotNull Path path) {
        removeVf(path);
        store.removeTestSetPackage(path);
    }

    public void removeTestRunPackage(final @NotNull Path path) {
        removeVf(path);
        store.removeTestRunPackage(path);
    }

    private void removeVf(final @NotNull Path path) {
        Services.getInstance(p, TreeUtilImpl.class).removeVf(p, this, path);
        VirtualFileManager.getInstance().syncRefresh();
    }

    public void moveNode(final @NotNull Path oldPath,
                         final @NotNull Path newPath,
                         final @Nullable Runnable onFinished) {
        final Path targetParent = newPath.getParent();
        if (targetParent == null) {
            if (onFinished != null) onFinished.run();
            return;
        }

        Services.getInstance(p, TreeUtilImpl.class).executeVfsAction(p, oldPath, targetParent, "Move Failed", (sourceVf, targetVf) -> {
            try {
                sourceVf.move(this, targetVf);
            } catch (final IOException ex) {
                Logger.error(ex.getMessage());
                throw new RuntimeException(ex);
            }
        }, () -> {
            store.renameNode(oldPath, newPath);
            Logger.info("Moved successfully to: " + newPath);
            if (onFinished != null) onFinished.run();
        }, onFinished);
    }

    public void copyNode(final @NotNull Path sourcePath, final @NotNull Path targetPath) {
        copyNodes(List.of(sourcePath), targetPath, null);
    }

    public void copyNode(final @NotNull Path sourcePath, final @NotNull Path targetPath, final Runnable onComplete) {
        copyNodes(List.of(sourcePath), targetPath, onComplete);
    }

    public void copyNodes(final @NotNull List<Path> sourcePaths, final @NotNull Path targetPath, final Runnable onComplete) {
        if (sourcePaths.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        final AtomicInteger pending = new AtomicInteger(sourcePaths.size());
        final Runnable operationFinished = () -> {
            if (pending.decrementAndGet() != 0) return;
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                refreshIndexedProject(targetPath);
                if (onComplete != null) ApplicationManager.getApplication().invokeLater(onComplete);
            });
        };
        for (Path sourcePath : sourcePaths) {
            Services.getInstance(p, TreeUtilImpl.class).executeVfsAction(p, sourcePath, targetPath, "Copy Failed", (sourceVf, targetVf) -> {
                try {
                    sourceVf.copy(this, targetVf, sourceVf.getName());
                } catch (final IOException ex) {
                    Logger.error(ex.getMessage());
                    throw new RuntimeException(ex);
                }
            }, operationFinished, operationFinished);
        }
    }

    private void refreshIndexedProject(final @NotNull Path changedPath) {
        store.getTestProjectsByPath().keySet().stream()
                .map(Path::of)
                .filter(changedPath::startsWith)
                .max(Comparator.comparingInt(Path::getNameCount)).ifPresent(scanCoordinator::rescanExclusively);
    }

    public void addTestProject(final @NotNull TestProjectDirectoryDto tp) {
        store.addTestProject(tp);
        store.addTestProjectMarker(p, tp);
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
            scanCoordinator.scan(projectPath);
        } catch (final Exception ex) {
            Logger.error("Failed to scan single project: " + ex.getMessage());
        }
    }

    public void persistTestProjectMarker(final @NotNull Project p, final @NotNull TestProjectDirectoryDto tp) {
        store.addTestProjectMarker(p, tp);
    }

    public void updateRunMarker(final @NotNull Project p, final @NotNull Path runPath, final @NotNull TestRunMarker marker) {
        store.updateRunMarker(p, runPath, marker);
    }

    public void renameNode(final @NotNull Path oldPath, final @NotNull Path newPath) {
        Services.getInstance(p, TreeUtilImpl.class).executeVfsAction(p, oldPath, "Rename Failed", vf -> {
            try {
                vf.rename(this, newPath.getFileName().toString());
            } catch (final IOException ex) {
                Logger.error(ex.getMessage());
                throw new RuntimeException(ex);
            }
        });
        store.renameNode(oldPath, newPath);
    }
}
