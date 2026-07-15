package org.testin.util.indexer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.TestRunMarker;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.TestRunDto;
import org.testin.pojo.dto.dirs.*;
import org.testin.settings.Setting;
import org.testin.util.FilesUtil;
import org.testin.util.logger.Log;
import org.testin.util.services.EditorStateService;
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

    private final Project project;
    private final IndexerDataStore store;
    private final IndexingScanner scanner;

    private final AtomicBoolean indexed = new AtomicBoolean(false);
    private final AtomicBoolean indexing = new AtomicBoolean(false);
    private final AtomicBoolean restoreEditorsOnComplete = new AtomicBoolean(true);

    private volatile CountDownLatch indexingLatch = new CountDownLatch(1);

    public ProjectIndexer(final @NotNull Project project) {
        this.project = project;
        this.store = new IndexerDataStore(project);
        this.scanner = new IndexingScanner(project, store);
    }

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
                Log.warn("No valid projects found at '" + absoluteRoot.toAbsolutePath() + "'");
                return;
            }

            indexingLatch = new CountDownLatch(validProjects.size());
            Log.info("Indexing " + validProjects.size() + " projects...");

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
                                } catch (Exception e) {
                                    Log.error("Failed to index project: " + projectName + " - " + e.getMessage());
                                }

                                indicator.setFraction(1.0);
                                indicator.setText("Done - " + projectName);
                            }

                            @Override
                            public void onSuccess() {
                                indexingLatch.countDown();
                                Log.info("Project '" + projectName + "' indexed.");

                                if (indexingLatch.getCount() == 0 && indexed.compareAndSet(false, true)) {
                                    indexing.set(false);

                                    logSummary();

                                    ApplicationManager.getApplication().invokeLater(() -> {
                                        if (restoreEditorsOnComplete.compareAndSet(true, true)) {
                                            Log.info("Indexing finished, restoring open editors.");
                                            Services.getInstance(project, EditorStateService.class)
                                                    .restoreOpenEditors();
                                        } else {
                                            Log.info("Indexing finished, skipping editor restore.");
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onThrowable(@NotNull Throwable error) {
                                indexingLatch.countDown();
                                Log.error("Error indexing '" + projectName + "': " + error.getMessage());
                            }
                        });
            }
        } catch (Exception e) {
            Log.error("indexWithProgress: " + e.getMessage());
            indexing.set(false);
        }
    }

    public void awaitIndexing() {
        if (indexed.get()) return;
        try {
            indexingLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isIndexed() {
        return indexed.get();
    }

    public void dispose() {
        store.clearAll();
        indexed.set(false);
        indexing.set(false);
        indexingLatch = new CountDownLatch(1);
        Log.info("Indexer disposed");
    }

    public void resetForReindex() {
        restoreEditorsOnComplete.set(false);
        store.clearAll();
        indexed.set(false);
        indexing.set(false);
        indexingLatch = new CountDownLatch(1);
        Log.info("Indexer reset for re-indexing");
    }

    private List<Path> collectValidProjects(final Path rootPath) {
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) return Collections.emptyList();

        final Path[] projectPaths;
        try (Stream<Path> dirs = Files.list(rootPath)) {
            projectPaths = dirs.filter(Files::isDirectory).toArray(Path[]::new);
        } catch (Exception e) {
            Log.error("Failed to list root directory: " + e.getMessage());
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
        final int setCount = store.getTestSetsByPath().size();
        final int runDirCount = store.getTestRunDirsByPath().size();
        final int setPkgCount = store.getTestSetPackagesByPath().size();
        final int runPkgCount = store.getTestRunPackagesByPath().size();

        final long estimatedBytes = estimateBytes(
                tcCount, trCount, projCount, setCount, runDirCount,
                setPkgCount, runPkgCount, store.getTestSetCaseIds().size());

        Log.info("Indexing complete: " +
                tcCount + " test cases, " +
                trCount + " test runs, " +
                projCount + " projects, " +
                setCount + " test sets, " +
                runDirCount + " test run dirs, " +
                setPkgCount + " test set packages, " +
                runPkgCount + " test run packages | " +
                formatBytes(estimatedBytes));
    }

    public List<TestCaseDto> getTestCasesForTestSet(final Path testSetPath) {
        return store.getTestCasesForTestSet(testSetPath);
    }

    public TestRunDto getTestRunForPath(final Path testRunPath) {
        return store.getTestRunForPath(testRunPath);
    }

    public TestCaseDto getTestCaseById(final UUID id) {
        return store.getTestCaseById(id);
    }

    public TestSetDirectoryDto getTestSetByPath(final Path path) {
        return store.getTestSetByPath(path);
    }

    public TestRunDirectoryDto getTestRunDirByPath(final Path path) {
        return store.getTestRunDirByPath(path);
    }

    public Map<String, TestProjectDirectoryDto> getTestProjectsByPath() {
        return store.getTestProjectsByPath();
    }

    public List<DirectoryDto> getChildren(final Path parentPath) {
        return store.getChildren(parentPath);
    }

    public void putTestCase(final Path testSetPath, final TestCaseDto tc) {
        store.putTestCase(testSetPath, tc);
    }

    public void removeTestCase(final Path testSetPath, final UUID tcId) {
        store.removeTestCase(testSetPath, tcId);
    }

    public void updateSequence(final Path testSetPath, final List<TestCaseDto> sortedList) {
        store.updateSequence(testSetPath, sortedList);
    }

    public void putTestRun(final Path testRunPath, final TestRunDto tr) {
        store.putTestRun(testRunPath, tr);
    }

    public void addTestProject(final TestProjectDirectoryDto tp) {
        store.addTestProject(tp);
    }

    public void addTestSet(final TestSetDirectoryDto ts) {
        store.addTestSet(ts);
    }

    public void addTestSetPackage(final TestSetPackageDirectoryDto tsp) {
        store.addTestSetPackage(tsp);
    }

    public void addTestRunDir(final TestRunDirectoryDto trd) {
        store.addTestRunDir(trd);
    }

    public void addTestRunPackage(final TestRunPackageDirectoryDto trp) {
        store.addTestRunPackage(trp);
    }

    public void persistTestProjectMarker(final Project project, final TestProjectDirectoryDto tp) {
        Services.getInstance(project, FilesUtil.class).write(project, tp.getPath().resolve(DirectoryType.TP.getMarker()), tp.getMarker());
    }

    public void updateRunMarker(final Project project, final Path runPath,
                                final TestRunMarker marker) {
        store.updateRunMarker(project, runPath, marker);
    }

    public void renameNode(final Path oldPath, final Path newPath) {
        store.renameNode(oldPath, newPath);
    }
}
