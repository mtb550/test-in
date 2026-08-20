package org.testin.indexer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.DirectoryMapper;
import org.testin.model.DirectoryType;
import org.testin.model.ProjectStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.*;
import org.testin.model.markers.TestRunMarker;
import org.testin.services.Services;
import org.testin.services.TestCaseCacheService;
import org.testin.setting.TestinRoot;
import org.testin.testproject.BoundTestProject;
import org.testin.util.EditorUtil;
import org.testin.util.Mapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

/**
 * The single owner of file access. No other class may read, write or execute
 * operations on virtual files (VFS) or physical files — everything goes
 * through the indexer so its cache objects stay authoritative and every read
 * is a fast in-memory lookup (e.g. {@link #nodeExists}). Exempt packages:
 * {@code git}, {@code importexport}, {@code logger}.
 * <p>
 * Ordering rule: the cache update (which may persist markers — and marker
 * writes create directories) runs only <b>after</b> the VFS operation
 * succeeded, never before. Violating this creates phantom directories and
 * "already exists in VFS" failures.
 */
@Service(Service.Level.PROJECT)
public final class ProjectIndexer {

    private final @NotNull Project p;
    private final @NotNull IndexerDataStore store;
    private final @NotNull ProjectScanCoordinator scanCoordinator;
    private final @NotNull AtomicBoolean indexed = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean indexing = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean restoreEditorsOnComplete = new AtomicBoolean(true);
    /**
     * All run-status disk writes go through this one sequential executor,
     * so writes can never interleave or race each other.
     */
    private final @NotNull ExecutorService runWriter =
            AppExecutorUtil.createBoundedApplicationPoolExecutor("Testin Run Status Writer", 1);
    private volatile @NotNull CountDownLatch indexingLatch = new CountDownLatch(1);

    public ProjectIndexer(final @NotNull Project p) {
        this.p = p;
        this.store = new IndexerDataStore(p);
        this.scanCoordinator = new ProjectScanCoordinator(new IndexingScanner(p, store));
    }

    public void indexWithProgress() {
        try {
            if (indexed.get() || indexing.getAndSet(true)) {
                return;
            }

            final Path absoluteRoot = absoluteRoot();
            if (absoluteRoot.toString().isEmpty()) {
                indexing.set(false);
                indexed.set(true);
                indexingLatch.countDown();
                return;
            }

            final List<Path> validProjects = boundOnly(collectValidProjects(absoluteRoot));
            if (validProjects.isEmpty()) {
                indexing.set(false);
                indexed.set(true);
                indexingLatch.countDown();
                Logger.warn("No valid projects found at '" + absoluteRoot.toAbsolutePath() + "'");
                return;
            }

            indexingLatch = new CountDownLatch(validProjects.size());
            Logger.info("Indexing " + validProjects.size() + " projects..");

            for (final Path projectPath : validProjects) {
                final String projectName = projectPath.getFileName().toString();

                ProgressManager.getInstance()
                        .run(new Task.Backgroundable(p, "Testin indexing - " + projectName, true) {
                            @Override
                            public void run(final @NotNull ProgressIndicator indicator) {
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
                            public void onThrowable(final @NotNull Throwable error) {
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

    public void resetForReindex() {
        restoreEditorsOnComplete.set(false);
        store.clearAll();
        indexed.set(false);
        indexing.set(false);
        indexingLatch = new CountDownLatch(1);
        Logger.info("Indexer reset for re-indexing");
    }

    /**
     * The Testin root as an absolute path, or the empty path when none is set.
     * A relative root is resolved against the open project, which is how it has
     * always been read - here rather than at each caller so that indexing and
     * the project listing can never disagree about where the root is.
     */
    private @NotNull Path absoluteRoot() {
        final Path rootPath = Services.getInstance(p, TestinRoot.class).getPath();
        if (rootPath.toString().isEmpty() || rootPath.isAbsolute()) return rootPath;

        return p.getBasePath() != null ? Path.of(p.getBasePath(), rootPath.toString()) : rootPath;
    }

    /**
     * Just the project this repository is bound to, when it is bound to one.
     * <p>
     * The reason the change is worth making: a tester with eleven test projects
     * under the root indexed all eleven on every open, and used one of them. An
     * unbound repository still indexes everything, because the picker that binds
     * it is the only screen that has a use for the others.
     */
    private @NotNull List<Path> boundOnly(final @NotNull List<Path> projects) {
        final String bound = Services.getInstance(p, BoundTestProject.class).name();
        if (bound.isEmpty()) return projects;

        final List<Path> scoped = projects.stream()
                .filter(path -> bound.equals(path.getFileName().toString()))
                .toList();

        if (scoped.isEmpty()) {
            Logger.warn("testin.yml names '" + bound + "', which is not a test project under the root");
            return scoped;
        }

        Logger.info("Indexing only the bound project '" + bound + "'");
        return scoped;
    }

    /**
     * Every test project folder under the root with the status its marker gives,
     * archived ones included. The listing behind the picker that binds a
     * repository, and behind the sentence that says why a bound project is not
     * showing - both of which have to know about a project the index skipped.
     * <p>
     * A directory read rather than a cache read, deliberately: it answers about
     * projects that were never indexed, which is exactly what the cache cannot do.
     */
    public @NotNull Map<String, ProjectStatus> testProjects() {
        final Map<String, ProjectStatus> byName = new LinkedHashMap<>();
        final Path root = absoluteRoot();
        if (root.toString().isEmpty()) return byName;

        for (final Path path : collectValidProjects(root)) {
            final String name = path.getFileName().toString();
            try {
                byName.put(name, Services.getInstance(p, DirectoryMapper.class)
                        .getTestProjectNode(p, path).getMarker().getStatus());

            } catch (final Exception ex) {
                // One project that will not be read must not cost the tester the
                // list of the others - the listing is what they choose from.
                Logger.warn("Could not read test project '" + name + "': " + ex.getMessage());
            }
        }

        return byName;
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

    public @NotNull TestRunDto getTestRunByPath(final @NotNull Path testRunPath) {
        return store.getTestRunByPath(testRunPath);
    }

    /**
     * A test case by id, empty when it is not indexed - a case deleted after a
     * run recorded it, or after the code that names it was generated.
     */
    public @NotNull Optional<TestCaseDto> findTestCase(final @NotNull UUID id) {
        return store.findTestCase(id);
    }

    public @NotNull TestSetDirectoryDto getTestSetByPath(final @NotNull Path path) {
        return store.getTestSetDirByPath(path);
    }

    public @NotNull TestRunDirectoryDto getTestRunDirByPath(final @NotNull Path path) {
        return store.getTestRunDirByPath(path);
    }

    /**
     * What a node holds, counted from the cache: test sets, test cases and test
     * runs anywhere beneath it.
     * <p>
     * For the question a confirmation has to answer before a tester agrees to a
     * removal - "and what goes with it?". Counted rather than guessed, because
     * the number is the whole point: removing a test project with two sets is a
     * different act from removing one with two hundred.
     */
    public @NotNull NodeContents contentsUnder(final @NotNull Path path) {
        final long sets = store.getTestSetsDirByPath().values().stream()
                .filter(set -> set.getPath().startsWith(path)).count();
        final long runs = store.getTestRunsDirByPath().values().stream()
                .filter(run -> run.getPath().startsWith(path)).count();
        final long cases = store.getTestCasesById().values().stream()
                .filter(tc -> tc.getParent().getPath().startsWith(path)).count();

        return new NodeContents(sets, cases, runs);
    }

    public @NotNull Map<String, TestProjectDirectoryDto> getTestProjectsByPath() {
        return store.getTestProjectsByPath();
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

    /**
     * Saves a case exactly as the import produced it, audit included. Every other
     * save stamps who did it and when; an import is the one case where that
     * belongs to the file being imported.
     */
    public void putImportedTestCase(final @NotNull Path testSetPath, final @NotNull TestCaseDto tc) {
        store.putImportedTestCase(testSetPath, tc);
    }

    public void removeTestCase(final @NotNull Path testSetPath, final @NotNull UUID tcId) {
        store.removeTestCase(testSetPath, tcId);

        // The completion cache is derived from the test cases, so it has to shrink
        // with them - otherwise a deleted description keeps being offered.
        Services.getInstance(p, TestCaseCacheService.class).reload(this::getAllTestCases);
    }

    /**
     * Every indexed test case, across all test sets.
     */
    public @NotNull List<TestCaseDto> getAllTestCases() {
        return List.copyOf(store.getTestCasesById().values());
    }

    public void updateSequence(final @NotNull Path testSetPath, final @NotNull List<TestCaseDto> orderedList,
                               final @NotNull List<TestCaseDto> moved) {
        store.updateSequence(testSetPath, orderedList, moved);
    }

    /**
     * Single-writer persistence for run results: the JSON snapshot is taken on
     * the calling (EDT) thread — so it can never observe a half-applied
     * mutation — and the sequential writer performs only the disk I/O, in
     * submission order.
     */
    public void persistRun(final @NotNull Path runPath, final @NotNull TestRunDto tr) {
        final byte[] snapshot;
        try {
            snapshot = Services.getInstance(p, Mapper.class).writeValueAsBytes(tr);
        } catch (final Exception ex) {
            Logger.error("Failed to snapshot test run data: " + ex.getMessage());
            return;
        }

        runWriter.execute(() -> {
            try {
                registerTestRun(runPath, tr);
                Services.getInstance(p, FilesUtil.class).write(p, runPath.resolve(runPath.getFileName() + ".json"), snapshot);
                Logger.trace("Run results persisted for " + runPath.getFileName());
            } catch (final Exception ex) {
                Logger.error("Failed to persist test run data: " + ex.getMessage());
            }
        });
    }

    /**
     * Same single-writer discipline for the run marker: snapshot on the
     * calling thread, sequential disk write.
     */
    public void persistRunMarker(final @NotNull Path runPath, final @NotNull TestRunMarker marker) {
        final byte[] snapshot;
        try {
            snapshot = Services.getInstance(p, Mapper.class).writeValueAsBytes(marker);
        } catch (final Exception ex) {
            Logger.error("Failed to snapshot run marker: " + ex.getMessage());
            return;
        }

        runWriter.execute(() -> {
            try {
                Services.getInstance(p, FilesUtil.class).write(p, runPath.resolve(DirectoryType.TR.getMarker()), snapshot);
                Logger.trace("Marker persisted -> " + marker.getStatus().getLabel());
            } catch (final Exception ex) {
                Logger.error("Failed to persist marker: " + ex.getMessage());
            }
        });
    }

    public void putTestRun(final @NotNull Path testRunPath, final @NotNull TestRunDto tr) {
        store.putTestRun(testRunPath, tr);
    }

    /**
     * Index-only registration; the caller persists the JSON itself.
     */
    public void registerTestRun(final @NotNull Path testRunPath, final @NotNull TestRunDto tr) {
        store.registerTestRun(testRunPath, tr);
    }

    /**
     * Deletes a test project from disk and from the cache, in that order.
     * <p>
     * The largest delete the plugin performs: the directory holds every test
     * set, case and run of that project, and removal is not recorded by the undo
     * service. What guards it is the confirmation, which counts what is inside
     * before it asks.
     */
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

    /**
     * Removes nothing, for the two containers the tree never deletes: Test Cases
     * and Test Runs go with their test project and never on their own.
     * <p>
     * The callback still runs, and reports false. RemoveAction counts
     * completions to know when to rebuild the tree, so a node that quietly did
     * nothing would leave the count short and the tree never rebuilt.
     * <p>
     * It must not be counted as removed either, or the tester is told a node
     * went that is still in front of them.
     */
    public void refuseRemove(final @NotNull Path path, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        Logger.info("Not removed: " + path.getFileName() + " is not removable from the tree");
        onRemoved.accept(false);
    }

    /**
     * Deletes on disk, refreshes, and only then updates the cache — the order
     * CLAUDE.md requires. The refresh is asynchronous now: the synchronous one
     * ran on the EDT, and a full VFS refresh there is a slow operation.
     * <p>
     * The cache update runs only when the deletion succeeded. It used to run either
     * way, so a file the VFS refused to delete was dropped from the cache and the
     * tree stopped showing a node that was still on disk (#66, F2).
     */
    private void removeVf(final @NotNull Path path, final @NotNull Runnable cacheUpdate,
                          final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        Services.getInstance(p, VfsExecutor.class).removeVf(p, this, path,
                deleted -> VirtualFileManager.getInstance().asyncRefresh(() -> {
                    if (deleted) cacheUpdate.run();
                    onRemoved.accept(deleted);
                }));
    }

    /**
     * Reports whether the node moved, not merely that the attempt is over. The
     * callback used to be one Runnable passed as both outcomes, so a caller that
     * wanted to confirm the move had to read the cache back afterward to find
     * out (#66, F2).
     */
    public void moveNode(final @NotNull Path oldPath,
                         final @NotNull Path newPath,
                         final @NotNull Consumer<@NotNull Boolean> onFinished) {
        final Path targetParent = newPath.getParent();
        if (targetParent == null) {
            Logger.warn("Move refused, target has no parent directory: " + newPath);
            onFinished.accept(false);
            return;
        }

        Services.getInstance(p, VfsExecutor.class).executeVfsAction(p, oldPath, targetParent, "Move Failed", (sourceVf, targetVf) -> {
            try {
                sourceVf.move(this, targetVf);
            } catch (final IOException ex) {
                Logger.error(ex.getMessage());
                throw new RuntimeException(ex);
            }
        }, () -> {
            store.renameNode(oldPath, newPath);
            Logger.info("Moved successfully to: " + newPath);
            onFinished.accept(true);
        }, () -> {
            onFinished.accept(false);
        });
    }

    /**
     * Copies each source into the target, and reports how many arrived — not how
     * many were attempted. Every copy runs its own VFS action and any of them can
     * fail on its own, so the count is the only honest answer; the callback used
     * to be a bare Runnable that fired either way, and callers could not tell a
     * finished copy from a failed one (#66, F2).
     */
    public void copyNodes(final @NotNull List<Path> sourcePaths, final @NotNull Path targetPath,
                          final @NotNull IntConsumer onComplete) {
        if (sourcePaths.isEmpty()) {
            onComplete.accept(0);
            return;
        }

        final AtomicInteger pending = new AtomicInteger(sourcePaths.size());
        final AtomicInteger copied = new AtomicInteger();

        // Both outcomes drain the counter, so the tree is still rebuilt when a
        // copy fails; only the success path raises the count.
        final Runnable operationFinished = () -> {
            if (pending.decrementAndGet() != 0) return;
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                refreshIndexedProject(targetPath);
                ApplicationManager.getApplication().invokeLater(() -> onComplete.accept(copied.get()));
            });
        };
        final Runnable operationSucceeded = () -> {
            copied.incrementAndGet();
            operationFinished.run();
        };

        for (final Path sourcePath : sourcePaths) {
            Services.getInstance(p, VfsExecutor.class).executeVfsAction(p, sourcePath, targetPath, "Copy Failed", (sourceVf, targetVf) -> {
                try {
                    sourceVf.copy(this, targetVf, sourceVf.getName());
                } catch (final IOException ex) {
                    Logger.error(ex.getMessage());
                    throw new RuntimeException(ex);
                }
            }, operationSucceeded, operationFinished);
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
        store.persistMarker(tp);
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

    /**
     * Writes any node's marker back through the indexer, which owns file access.
     * Every marker write goes through here, whichever node it belongs to.
     * Writing the file, invalidating the cached children and refreshing the VFS
     * are one act: a caller that does only the first leaves a file the IDE never
     * hears about, and the Git paths read through the IDE.
     */
    public void persistMarker(final @NotNull DirectoryDto dto) {
        store.persistMarker(dto);
    }

    /**
     * Reads a node's marker, falling back to a default instance when the file is
     * missing or unreadable. The indexer owns both directions of the marker round
     * trip; nothing outside it opens a marker file (#49).
     */
    public <M> @NotNull M readMarker(final @NotNull Path dirPath, final @NotNull String markerFileName,
                                     final @NotNull Class<M> type, final @NotNull String kind, final @NotNull String name) {
        return store.readMarker(dirPath, markerFileName, type, kind, name);
    }

    /**
     * The node at a path, whatever kind it is, empty when nothing is indexed
     * there. Saves a caller that only has a path from having to know which kind
     * of node to ask for.
     * <p>
     * The one path lookup that answers rather than promises: its callers ask
     * about a path they remembered - editors to reopen from a previous session,
     * a path typed into settings - and what was there last time may not be there
     * now. Every other lookup is keyed by something on the screen and returns
     * the node (#71).
     */
    public @NotNull Optional<DirectoryDto> find(final @NotNull Path path) {
        return store.findByPath(path);
    }

    public void updateRunMarker(final @NotNull Project p, final @NotNull Path runPath, final @NotNull TestRunMarker marker) {
        store.updateRunMarker(p, runPath, marker);
    }

    /**
     * Cache lookup, no disk access: true when a tree node exists at the path.
     */
    public boolean nodeExists(final @NotNull Path path) {
        return store.findByPath(path).isPresent();
    }

    /**
     * VFS refresh of a directory — file access stays inside the indexer, and
     * callers (often on the EDT) are never blocked on disk.
     */
    public void refreshDirectory(final @NotNull Path path) {
        store.refreshDir(path);
    }

    /**
     * VFS refresh of one file the plugin wrote outside the VFS, so it appears in
     * the Project view without waiting for the IDE to notice it by itself.
     */
    public void refreshFile(final @NotNull Path file) {
        store.refreshFile(file);
    }

    /**
     * The callback runs only when the rename succeeded. Unlike the copy and move
     * forms, this needs no success flag: the whole body is one VFS operation, and
     * {@code executeVfsAction} reports and swallows a failure before the cache
     * update and the callback are reached.
     */
    public void renameNode(final @NotNull Path oldPath, final @NotNull Path newPath, final @NotNull Runnable onFinished) {
        Services.getInstance(p, VfsExecutor.class).executeVfsAction(p, oldPath, vf -> {
            try {
                vf.rename(this, newPath.getFileName().toString());
            } catch (final IOException ex) {
                Logger.error(ex.getMessage());
                throw new RuntimeException(ex);
            }

            // The cache update persists the touched marker at the NEW path, and
            // that write creates directories. So it must run only after the VFS
            // rename succeeded: otherwise the target directory already exists
            // and the rename fails with "already exists in VFS".
            store.renameNode(oldPath, newPath);
            onFinished.run();
        });
    }

    /**
     * How much a node holds, for a caller that has to say it out loud.
     */
    public record NodeContents(long testSets, long testCases, long testRuns) {

        /**
         * The sentence a confirmation shows, or blank when the node holds
         * nothing - "and nothing else goes with it" is not worth a line.
         */
        public @NotNull String describe() {
            if (testSets == 0 && testCases == 0 && testRuns == 0) return "";

            return "Holds " + testSets + " test set" + plural(testSets)
                    + ", " + testCases + " test case" + plural(testCases)
                    + " and " + testRuns + " test run" + plural(testRuns);
        }

        private @NotNull String plural(final long count) {
            return count == 1 ? "" : "s";
        }
    }
}
