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
import org.testin.logger.Logger;
import org.testin.model.DirectoryType;
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
import org.testin.model.markers.TestRunMarker;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.setting.TestinRoot;
import org.testin.testproject.BoundTestProject;
import org.testin.editor.LastOpenEditors;
import org.testin.util.Bundle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The single owner of file access. No other class may read, write or execute
 * operations on virtual files (VFS) or physical files — everything goes
 * through the indexer so its cache objects stay authoritative and every read
 * is a fast in-memory lookup (e.g. {@link #nodeExists}). The packages exempt
 * from this, and why, are listed in {@code docs/ARCHITECTURE.md} and only there:
 * this said three while that said eight (#66, finding 228).
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
    private final @NotNull RunWriter runWriter;
    private final @NotNull SyncFiles syncFiles;

    /**
     * UC-SHARE-019, Rule-SHARE-003.
     * <p>
     * The test projects a sync is writing into right now, each with the run
     * changes waiting for it to be read again. Guarded by itself.
     */
    private final @NotNull Map<Path, List<Runnable>> heldForSync = new HashMap<>();
    private final @NotNull NodeFiles nodeFiles;
    /**
     * Counts the whole indexing pass, not the projects in it.
     * <p>
     * It used to be replaced with one latch per project once the list was known,
     * so a thread that had already entered {@link #awaitIndexing} was left
     * waiting on a latch nobody could reach (#66, finding 67).
     */
    private volatile @NotNull CountDownLatch indexingLatch = new CountDownLatch(1);

    public ProjectIndexer(final @NotNull Project p) {
        this.p = p;
        this.store = new IndexerDataStore(p);
        this.scanCoordinator = new ProjectScanCoordinator(new IndexingScanner(p, store));
        this.runWriter = new RunWriter(p, store);
        this.syncFiles = new SyncFiles(p, runWriter);
        this.nodeFiles = new NodeFiles(p, this, store);
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

            // The pass's own, so a pass a Refresh has since replaced counts down
            // nothing of the pass that replaced it.
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

            // Nothing below is going to count the latch down now, and
            // awaitIndexing blocks on it. One failed re-index used to leave
            // Refresh, the branch dropdown and the welcome screen's links dead
            // for the rest of the session (#66, finding 67).
            indexingLatch.countDown();
        }
    }

    /**
     * One project's scan is over, whichever way it ended.
     * <p>
     * The counter and the latch are the pass's own, taken when it started. A
     * Refresh or a branch switch pressed while a pass is still scanning resets
     * the index and starts a second pass, and the first pass's scans still
     * finish. They used to count down one shared counter and whichever latch was
     * current, so the second pass was told it had finished when the first one
     * did, and the tree and the editors redrew over a project still being read
     * (#312, A2).
     *
     * @return whether that was the last project of a pass that is still the
     * current one - a pass that has been replaced has nothing left to finish
     */
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

    /**
     * Blocks until the index is built.
     * <p>
     * Only from a thread holding no lock. A read action that blocks here holds
     * the read lock for as long as the wait, and every write action in the IDE
     * queues behind it - including the one {@code DumbService} takes on the EDT
     * to start indexing. The tree used to call this from its Invoker, which the
     * platform runs inside a read action, and a 32-second wait froze the IDE
     * until it was killed (#89).
     * <p>
     * Checked rather than documented, because the comment was not enough: the
     * call site that did it looked exactly like the two that are safe.
     */
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
        return Services.getInstance(p, TestinRoot.class).absolutePath();
    }

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-006.
     * <p>
     * Just the project this repository is bound to, when it is bound to one.
     * <p>
     * The reason the change is worth making: a tester with eleven test projects
     * under the root indexed all eleven on every open, and used one of them. An
     * unbound repository still indexes everything, because the picker that binds
     * it is the only screen that has a use for the others.
     */
    private @NotNull List<Path> boundOnly(final @NotNull List<Path> projects) {
        final @NotNull String bound = Services.getInstance(p, BoundTestProject.class).name();
        if (bound.isEmpty()) return projects;

        final @NotNull List<Path> scoped = projects.stream()
                .filter(this::isBound)
                .toList();

        if (scoped.isEmpty()) {
            Logger.warn("testin.yml names '" + bound + "', which is not a test project under the root");
            return scoped;
        }

        Logger.info("Indexing only the bound project '" + bound + "'");
        return scoped;
    }

    /**
     * Whether this is the project {@code testin.yml} binds - or any project,
     * when it binds none. Asked by startup and by a rescan alike.
     */
    private boolean isBound(final @NotNull Path projectPath) {
        final @NotNull String bound = Services.getInstance(p, BoundTestProject.class).name();
        return bound.isEmpty() || bound.equals(projectPath.getFileName().toString());
    }

    /**
     * Every test project folder under the root with the status its marker gives,
     * inactive ones included. The listing behind the picker that binds a
     * repository, and behind the sentence that says why a bound project is not
     * showing - both of which have to know about a project the index skipped.
     * <p>
     * A directory read rather than a cache read, deliberately: it answers about
     * projects that were never indexed, which is exactly what the cache cannot do.
     */
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
                // One project that will not be read must not cost the tester the
                // list of the others - the listing is what they choose from.
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
        Arrays.stream(projectPaths).forEach(p -> {
            if (isTestProjectFolder(p)) {
                valid.add(p);
            } else {
                Logger.warn("Skipping directory without a " + DirectoryType.TP.getMarker()
                        + " marker (not a test project): " + p);
            }
        });
        return valid;
    }

    /**
     * Whether a folder under the Testin root is a test project: it carries the
     * {@code .tp} marker. Asked by startup and by a rescan alike.
     */
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

    /**
     * UC-INTERNAL-006, Rule-INTERNAL-046.
     * <p>
     * How many test cases a test set holds.
     * <p>
     * Counted from the ids the store already keeps rather than from the cases:
     * {@link #getTestCasesForTestSet} builds the list and sorts it into rank
     * order, and sorting 2,770 cases to produce a number nobody reads is work
     * for nothing.
     * <p>
     * A node that holds no cases of its own answers zero, so a walk asks every
     * node it meets the same question instead of first asking what kind it is.
     */
    public long caseCountOf(final @NotNull Path testSetPath) {
        return store.getTestSetCaseIds().getOrDefault(testSetPath.toString(), List.of()).size();
    }

    /**
     * Rule-TREE-PANEL-008.
     * <p>
     * Every test case under this node, in tree order: a test set's own cases, and
     * those of every test set beneath a package.
     * <p>
     * No instanceof and no special case for a package: a node that holds no cases
     * of its own answers with an empty list, so one walk serves a test set, a
     * package of them, and the Test Cases root alike.
     * <p>
     * Retired branches are left out. A deprecated test set, or anything under an
     * archived package, is not current work - the same rule that keeps it out of
     * the case selection when a run is configured (#68). A retired node the
     * tester picked out themselves is still walked: they asked for it by name.
     */
    public @NotNull List<TestCaseDto> getTestCasesUnder(final @NotNull DirectoryDto dir) {
        final @NotNull List<TestCaseDto> cases = new ArrayList<>(getTestCasesForTestSet(dir.getPath()));

        for (final DirectoryDto child : getChildren(dir.getPath())) {
            if (child.isRetired()) continue;

            cases.addAll(getTestCasesUnder(child));
        }

        return cases;
    }

    public @NotNull TestRunDto getTestRunByPath(final @NotNull Path testRunPath) {
        return withRemovedMarked(store.getTestRunByPath(testRunPath));
    }

    /**
     * UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126.
     * <p>
     * Marks each result whose test case is no longer indexed as removed, on the
     * run about to be handed out. Decided here, on every read, because the
     * indexer is the one place that knows which test cases exist: a case deleted
     * or put back since the last read is answered by the next one. The mark is
     * never written, so the file keeps the verdict (#66, finding 110).
     */
    private @NotNull TestRunDto withRemovedMarked(final @NotNull TestRunDto run) {
        run.getResults().forEach(item -> item.setRemoved(store.findTestCase(item.getId()).isEmpty()));
        return run;
    }

    /**
     * UC-VIEW-PANEL-008, Rule-VIEW-PANEL-065.
     * <p>
     * Every indexed test run, by the path it sits at - the run half of
     * {@link #getAllTestCases()}.
     * <p>
     * By path because a run does not carry its own name: it is the folder's, and
     * the folder is the key this cache is already held under. A tab that wants
     * to say which cycle a bug was found in needs both.
     */
    public @NotNull Map<Path, TestRunDto> getAllTestRuns() {
        return store.getTestRunsByPath().entrySet().stream()
                .collect(Collectors.toMap(entry -> Path.of(entry.getKey()), entry -> withRemovedMarked(entry.getValue())));
    }

    /**
     * UC-INTERNAL-006, Rule-INTERNAL-051.
     * <p>
     * The run recorded at this path, and empty when the tree has the directory
     * but nothing could be read out of it - a run whose JSON is missing, or one
     * that would not parse, both of which the scan logs and carries on past.
     * <p>
     * {@link #getTestRunByPath} is for callers that cannot continue without a
     * run and should fail loudly; this is for the ones that can say so instead.
     * The Details popup is the second kind: a run it cannot read is still a node
     * whose name, path and audit it can show.
     */
    public @NotNull Optional<TestRunDto> findTestRun(final @NotNull Path testRunPath) {
        return store.findTestRun(testRunPath).map(this::withRemovedMarked);
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

    public @NotNull Map<String, TestProjectDirectoryDto> getTestProjectsByPath() {
        return store.getTestProjectsByPath();
    }

    public boolean projectExists(final @NotNull Path projectPath) {
        return Files.isDirectory(projectPath);
    }

    public @NotNull List<DirectoryDto> getChildren(final @NotNull Path parentPath) {
        return store.getChildren(parentPath);
    }

    /**
     * UC-INTERNAL-004, Rule-INTERNAL-033.
     * <p>
     * Saves a test case, and says whether it did - false when the file already
     * holds it exactly, which is a tester who opened a field, changed nothing and
     * pressed Enter (#164), and false when the file could not be written, which
     * the writer has already said (#66, finding 163). Either way there is
     * nothing for the caller to confirm, regenerate or take back.
     */
    public boolean putTestCase(final @NotNull Path testSetPath, final @NotNull TestCaseDto tc) {
        return store.putTestCase(testSetPath, tc);
    }

    /**
     * UC-INTERNAL-004, Rule-INTERNAL-035.
     * <p>
     * Saves a case exactly as it was given, audit included. Every ordinary save
     * stamps who did it and when; these are the saves where that would be a lie.
     * <p>
     * An import writes the audit the file being imported carries. An undo writes
     * the audit the case had before the change it is taking back - stamping it
     * would record the tester as having modified a case at the moment they
     * un-modified it (#164, #165). A cut being pasted is the same case in a new
     * place, and the ordinary save would record the paster as its creator (#66,
     * finding 114).
     */
    public boolean putTestCaseVerbatim(final @NotNull Path testSetPath, final @NotNull TestCaseDto tc) {
        return store.putTestCaseVerbatim(testSetPath, tc);
    }

    public void removeTestCase(final @NotNull Path testSetPath, final @NotNull UUID tcId) {
        store.removeTestCase(testSetPath, tcId);

        // The completion cache is derived from the test cases, so it has to shrink
        // with them - otherwise a deleted description keeps being offered.
        Services.getInstance(p, TestCaseValues.class).reload(this::getAllTestCases);
    }

    /**
     * Every indexed test case, across all test sets.
     */
    public @NotNull List<TestCaseDto> getAllTestCases() {
        return List.copyOf(store.getTestCasesById().values());
    }

    /**
     * Every indexed node, of every kind - the other half of what the plugin
     * knows, beside {@link #getAllTestCases()} (#29).
     * <p>
     * A view of what the scan already holds, so it costs one pass over memory
     * and cannot go stale: there is nothing here to update when a node is
     * created, moved or removed, because the maps it reads are the ones those
     * operations already change.
     */
    public @NotNull List<DirectoryDto> getAllNodes() {
        return List.copyOf(store.allDirectories());
    }

    // UC-INTERNAL-004, Rule-INTERNAL-031
    public void updateSequence(final @NotNull Path testSetPath, final @NotNull List<TestCaseDto> orderedList, final @NotNull List<TestCaseDto> moved) {
        store.updateSequence(testSetPath, orderedList, moved);
    }

    /**
     * The run's results, written by the one writer that owns the file - see
     * {@link RunWriter} for why there is only one and why the snapshot is taken
     * here rather than there.
     * <p>
     * Private: every write of an existing run goes through {@link #changeRun} or
     * {@link #saveRun}, which a sync can hold. Written from outside, a run an
     * editor held put the run from before a sync back over the one that arrived
     * (#66, finding 152).
     */
    private void persistRun(final @NotNull Path runPath, final @NotNull TestRunDto tr) {
        runWriter.persist(runPath, tr);
    }

    /**
     * UC-SHARE-019, Rule-SHARE-003.
     * <p>
     * Changes a run as the index holds it when the change is applied, and writes
     * it.
     * <p>
     * <b>Held while a sync of the run's test project is under way</b> - see
     * {@link #whileSyncing}. Until the sync has read back what it brought, the
     * index still holds the run as it was, so a change written then put the older
     * run back over the one that arrived (#66, findings 129 and 144). Held, the
     * change is applied to the run that arrived once the sync lets go, on the
     * EDT, where a verdict is recorded. A run the sync took away is not there to
     * change, and the log says so.
     * <p>
     * <b>And shown at once.</b> Meanwhile the change is applied, without being
     * written, to the run the index holds now, so the grid and the panels show
     * what the tester just recorded rather than nothing until the sync's refresh
     * (#66, finding 146). It records the same thing when it is applied again.
     */
    public void changeRun(final @NotNull Path runPath, final @NotNull Consumer<TestRunDto> change) {
        whenTheSyncLetsGo(runPath,
                () -> findTestRun(runPath).ifPresentOrElse(run -> {
                    change.accept(run);
                    persistRun(runPath, run);
                }, () -> Logger.warn("Test run no longer indexed, so a change to it was dropped: " + runPath.getFileName())),
                () -> findTestRun(runPath).ifPresent(change));
    }

    /**
     * UC-SHARE-019, Rule-SHARE-003.
     * <p>
     * Changes a run's marker as the index holds it when the change is applied,
     * and writes it - the same discipline as {@link #changeRun}, for the other
     * half of what a run is.
     * <p>
     * It used to take a marker and write it straight through. So completing or
     * closing a run during a sync of its project wrote the status at once while
     * the cases it belongs to waited, and the run could sit Completed over cases
     * still Pending, or keep its old status over cases already turned Untested,
     * until the sync let the rest through (#312, A4). A status and the results it
     * describes are one change, and they are held as one now.
     * <p>
     * A change rather than a marker, for the reason {@link #changeRun} takes one:
     * the marker to write is the one the index holds when the write happens, not
     * the object the caller was looking at before the sync replaced it.
     */
    public void changeRunMarker(final @NotNull Path runPath, final @NotNull Consumer<TestRunMarker> change) {
        // Both halves ask whether the run is still there. The one shown at once
        // used the getter that throws by design, so setting a run's status while
        // a sync removed it was an internal error where the other half wrote a
        // log line (#66, finding 204).
        whenTheSyncLetsGo(runPath,
                () -> store.findTestRunDir(runPath).ifPresentOrElse(dir -> {
                    final @NotNull TestRunMarker marker = dir.getMarker();
                    change.accept(marker);
                    runWriter.persistMarker(runPath, marker);
                }, () -> Logger.warn("Test run no longer indexed, so a change to its marker was dropped: " + runPath.getFileName())),
                () -> store.findTestRunDir(runPath).ifPresent(dir -> change.accept(dir.getMarker())));
    }

    /**
     * Runs the write now, or queues it behind a sync of the run's test project
     * and does {@code meanwhile} instead - which is what puts the change on
     * screen without writing it (#66, finding 146).
     * <p>
     * One method for the results and the marker, so the two cannot start being
     * held differently: half a change through and half of it waiting is exactly
     * what A4 was.
     */
    private void whenTheSyncLetsGo(final @NotNull Path runPath, final @NotNull Runnable apply, final @NotNull Runnable meanwhile) {
        synchronized (heldForSync) {
            final @NotNull Optional<List<Runnable>> waiting = heldForSync.entrySet().stream()
                    .filter(entry -> runPath.startsWith(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst();

            // Written inside the lock when nothing holds it. Decided here and
            // written after it, a sync that took its hold in between had its run
            // file overwritten by the change it was meant to hold (#66, finding
            // 203). The write only queues its bytes, so nothing waits in here.
            if (waiting.isEmpty()) {
                apply.run();
                return;
            }

            waiting.orElseThrow().add(apply);
        }

        meanwhile.run();
    }

    /**
     * Writes a run as the index holds it, under the same hold as
     * {@link #changeRun}: for a caller whose change is already on that run, such
     * as an open editor's execution stamps (#66, finding 152).
     */
    public void saveRun(final @NotNull Path runPath) {
        changeRun(runPath, run -> {
        });
    }

    /**
     * Registers the run and writes it, through the one writer that owns the file.
     * <p>
     * It used to write straight from the calling thread while
     * {@link #persistRun} queued its writes - so the run JSON had two writers
     * and no order between them. Saving Result Analysis took the direct path on
     * the UI thread while a verdict recorded moments earlier could still be
     * queued, holding a snapshot taken before the analysis existed; the queued
     * write then landed second and silently restored the older file. The tester
     * found their analysis gone after the next reload.
     * <p>
     * The registration stays immediate. Creating a run needs the index to know
     * about it on the next line, and only the disk write belongs in the queue.
     * This is the one door that puts a run into the index: {@link #persistRun}
     * refuses a run that is not there (#66, finding 143).
     */
    public void putTestRun(final @NotNull Path testRunPath, final @NotNull TestRunDto tr) {
        runWriter.create(testRunPath, tr);
    }

    /**
     * UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219.
     * <p>
     * Keeps newly pasted screenshots as PNG files beside the run, each under a
     * short name of its own, and answers the names, in order, for the run item
     * to hold (#313).
     */
    public @NotNull List<String> storeScreenshots(final @NotNull Path runPath, final @NotNull List<byte[]> pngs) {
        return runWriter.storeScreenshots(runPath, pngs);
    }

    /**
     * The PNG bytes of one screenshot a run item names, and none when its file
     * is missing.
     */
    public byte @NotNull [] screenshot(final @NotNull Path runPath, final @NotNull String name) {
        return runWriter.readScreenshot(runPath, name);
    }

    /**
     * The PNG bytes of every screenshot a run item names, in its order.
     */
    public @NotNull List<byte[]> screenshots(final @NotNull Path runPath, final @NotNull TestRunItems item) {
        return item.getScreenshots().stream().map(name -> screenshot(runPath, name)).toList();
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
     * set, case and run of that project. It goes to the recycle bin, and the
     * removal keeps a copy aside first so CTRL+Z can put it back
     * (UC-INTERNAL-005); the confirmation still counts what is inside before it
     * asks.
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
     * UC-TREE-PANEL-012, Rule-TREE-PANEL-042.
     * <p>
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
     * Deletes the node's files, then updates the cache - see {@link NodeFiles},
     * which owns that order and the reason for it.
     */
    private void removeVf(final @NotNull Path path, final @NotNull Runnable cacheUpdate, final @NotNull Consumer<@NotNull Boolean> onRemoved) {
        nodeFiles.remove(path, cacheUpdate, onRemoved);
    }

    /**
     * Reports whether the node moved, not merely that the attempt is over.
     */
    public void moveNode(final @NotNull Path oldPath, final @NotNull Path newPath, final @NotNull Consumer<@NotNull Boolean> onFinished) {
        nodeFiles.move(oldPath, newPath, onFinished);
    }

    /**
     * Copies each source into the target, and reports how many arrived - not how
     * many were attempted.
     */
    public void copyNodes(final @NotNull List<Path> sourcePaths, final @NotNull Path targetPath, final @NotNull IntConsumer onComplete) {
        nodeFiles.copy(sourcePaths, targetPath, onComplete);
    }

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-011.
     * <p>
     * A test case is a {@code .json} directly inside a test set, which is the
     * rule the scan reads by - so the copy and the scan agree about what a test
     * case is. A run's file is named for its folder and sits inside a test run,
     * and a marker is not JSON, so neither answers true.
     * <p>
     * It used to ask whether the file name parsed as a UUID. That is how Testin
     * names the files it writes, but not the only legal name: a case file named
     * by hand is read by the scan, which takes its identity from inside the file
     * instead. So the scan indexed it and this passed over it, and copying its
     * test set left the copy carrying the original's id - two files claiming one
     * case, where editing either edited both (#288).
     * <p>
     * The test-set check is injected so the rule stays testable without an
     * indexer - the same reason {@code TreeTransferHandler.isValidDestination}
     * takes its occupied check as a parameter.
     */
    static boolean isCaseFile(final @NotNull Path file, final @NotNull Predicate<Path> isTestSet) {
        return file.getFileName().toString().endsWith(".json") && isTestSet.test(file.getParent());
    }

    /**
     * UC-INTERNAL-005, Rule-INTERNAL-037, Rule-INTERNAL-041.
     * <p>
     * Keeps a copy of a node aside before it is removed, so the removal can be
     * taken back, and answers where it was kept. Nothing when the copy could not
     * be made, in which case the removal still happens and simply cannot be
     * undone - which is what every removal did before (#165).
     */
    public @NotNull Optional<Path> keepAside(final @NotNull Path node) {
        return Services.getInstance(DeletedNodes.class).keep(node);
    }

    /**
     * UC-INTERNAL-005, Rule-INTERNAL-042.
     * <p>
     * Puts a removed node back from the copy kept aside for it, and re-reads the
     * test project it landed in so the tree, the open editors and the caches all
     * agree with the disk again.
     */
    public boolean restoreNode(final @NotNull Path kept, final @NotNull Path original) {
        if (!Services.getInstance(DeletedNodes.class).putBack(kept, original)) return false;

        refreshDirectory(original);
        refreshIndexedProject(original);
        return true;
    }

    /**
     * UC-INTERNAL-005, Rule-INTERNAL-043.
     * <p>
     * Nobody can reach the operation that was holding this any more.
     */
    public void forgetKept(final @NotNull Path kept) {
        Services.getInstance(DeletedNodes.class).forget(kept);
    }

    /**
     * Re-reads the test project a change landed in, innermost first. Package
     * private because {@link NodeFiles} reaches back for it when a copy has
     * finished arriving.
     */
    void refreshIndexedProject(final @NotNull Path changedPath) {
        testProjectHolding(changedPath).ifPresent(scanCoordinator::rescanExclusively);
    }

    /**
     * The folder of the indexed test project this path sits in, innermost first,
     * and empty when none holds it.
     */
    private @NotNull Optional<Path> testProjectHolding(final @NotNull Path path) {
        return store.getTestProjectsByPath().keySet().stream()
                .map(Path::of)
                .filter(path::startsWith)
                .max(Comparator.comparingInt(Path::getNameCount));
    }

    /**
     * UC-INTERNAL-004, Rule-INTERNAL-034.
     * <p>
     * Where a test case's file sits: the test project folder holding it, and the
     * file's path inside that folder - what a bug report links to in the test
     * project's repository (#28). Empty when no indexed test project holds the
     * case's test set.
     * <p>
     * Answered here because the file's name is the indexer's to decide; a link
     * builder spelling {@code <id>.json} itself would be one more copy of it.
     */
    public @NotNull Optional<TestCaseFile> testCaseFile(final @NotNull TestCaseDto tc) {
        final @NotNull Path file = store.testCaseFileOf(tc);
        return testProjectHolding(file).map(testProject -> new TestCaseFile(testProject, testProject.relativize(file)));
    }

    /**
     * UC-TREE-PANEL-002.
     * <p>
     * A new node, and whether its markers landed - the creators confirm only a
     * node that was made (#312, A5).
     */
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

    /**
     * Every file in a test project, by the path a server names it with (#94) -
     * see {@link SyncFiles} for why a sync deals in files rather than nodes.
     */
    public @NotNull Map<String, byte[]> filesUnder(final @NotNull Path projectPath) {
        return syncFiles.under(projectPath);
    }

    /**
     * UC-INTERNAL-003, Rule-INTERNAL-017.
     * <p>
     * Whether the file belongs to Git rather than to the test project.
     * <p>
     * A repository's own directory is not test data. Its files change on every
     * command - HEAD, FETCH_HEAD, the index, the logs - so carrying them to a
     * server means a conflict on every sync forever, and writing one machine's
     * copy over another's would break the repository rather than share it.
     */
    public static boolean isGitsOwn(final @NotNull String relative) {
        return relative.equals(".git") || relative.startsWith(".git/");
    }

    /**
     * UC-SHARE-019, Rule-SHARE-003.
     * <p>
     * Runs one sync of a test project with every change to its runs held, and
     * applies what was held once the sync is over.
     * <p>
     * From before the sync reads the project's files until after its last scan.
     * The hold started only when the incoming files were accepted, so a verdict
     * recorded while the sync was still planning or moving files was written at
     * once, and the file that arrived then replaced it (#66, finding 144). What
     * was held is applied on the EDT, where a verdict is recorded, ahead of
     * anything the caller redraws once this returns.
     */
    public <T> @NotNull T whileSyncing(final @NotNull Path projectPath, final @NotNull Supplier<@NotNull T> sync) {
        synchronized (heldForSync) {
            heldForSync.putIfAbsent(projectPath, new ArrayList<>());
        }

        try {
            return sync.get();
        } finally {
            final @NotNull List<Runnable> held;
            synchronized (heldForSync) {
                held = Optional.ofNullable(heldForSync.remove(projectPath)).orElse(List.of());
            }

            if (!held.isEmpty()) ApplicationManager.getApplication().invokeLater(() -> held.forEach(Runnable::run));
        }
    }

    /**
     * Writes what arrived from a server into the project, and reads the project
     * again. A sync calls it inside {@link #whileSyncing}, which holds the run
     * changes made meanwhile.
     * <p>
     * The scan is not optional: these writes are claimed as our own, so the
     * watcher rightly ignores them, and no other path will ever index what they
     * put on disk. Without it the next tree refresh repainted the old cache and
     * the downloaded cases stayed invisible until a manual refresh (#118) - the
     * mirror {@link #removeIncoming} always scanned.
     */
    public @NotNull Set<String> acceptIncoming(final @NotNull Path projectPath, final @NotNull Map<String, byte[]> files) {
        final @NotNull Map<String, Future<Boolean>> writes = syncFiles.accept(projectPath, files);

        // A run's incoming files went through the run writer's queue; the scan
        // reads them once they have landed rather than racing them (#66,
        // finding 121). Every write has answered by then, so which of them did
        // not land is read afterwards rather than waited for.
        runWriter.awaitQueued();

        final @NotNull Set<String> notWritten = SyncFiles.notLanded(projectPath, writes);
        scanSingleProject(projectPath);
        return notWritten;
    }

    /**
     * Removes files the server no longer holds, once the tester has agreed to
     * it, and reads the project again - the mirror of {@link #acceptIncoming},
     * scanning for the same reason.
     */
    public void removeIncoming(final @NotNull Path projectPath, final @NotNull Collection<String> relatives) {
        syncFiles.remove(projectPath, relatives);

        // A run's files were removed through the run writer's queue; the scan
        // reads the project once they are gone (#66, finding 130).
        runWriter.awaitQueued();

        scanSingleProject(projectPath);
    }

    /**
     * UC-INTERNAL-003, Rule-INTERNAL-016.
     * <p>
     * Reads a test project again after a change on disk, or forgets it when the
     * folder is not one Testin reads: it has no {@code .tp} marker, or it is not
     * the project {@code testin.yml} binds.
     * <p>
     * The watcher knows only a path, and a scan puts whatever folder it is given
     * into the index as a test project - so a folder of notes beside the
     * projects became one, and a project {@code testin.yml} leaves out was read
     * in although startup had left it out (#66, finding 120). These are the two
     * questions startup asks through {@link #collectValidProjects} and
     * {@link #boundOnly}; a folder that has stopped being a test project, its
     * marker gone with it, is dropped rather than read back.
     */
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

    /**
     * UC-INTERNAL-003, Rule-INTERNAL-021.
     * <p>
     * The same pass, reporting into a bar the tester can watch and stop.
     * <p>
     * The indicator is carried rather than made here because the scan is what
     * knows the answer: which test set it is on, how far through it is, and
     * whether Cancel has been pressed. A caller that has a bar hands it over; a
     * caller with nowhere to show one passes an empty indicator, which reports
     * nothing and is never cancelled, so both go down one path (#20).
     */
    public void scanSingleProject(final @NotNull Path projectPath, final @NotNull ProgressIndicator indicator) {
        Logger.info("Scanning single project: " + projectPath.getFileName());
        try {
            scanCoordinator.scan(projectPath, indicator);
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
     * <p>
     * Answers whether the marker landed (#312, A6).
     */
    public boolean persistMarker(final @NotNull DirectoryDto dto) {
        return store.persistMarker(dto);
    }

    /**
     * Reads a node's marker, falling back to a default instance when the file is
     * missing or unreadable. The indexer owns both directions of the marker round
     * trip; nothing outside it opens a marker file (#49).
     */
    public <M> @NotNull M readMarker(final @NotNull Path dirPath, final @NotNull DirectoryType kind, final @NotNull Class<M> markerClass, final @NotNull String name) {
        return store.readMarker(dirPath, kind, markerClass, name);
    }

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-014.
     * <p>
     * The nodes drawn with default values because their marker would not parse,
     * and forgotten in the asking, so the scan that reports them reports each
     * one once.
     */
    public @NotNull List<String> takeDamagedMarkers() {
        return store.takeDamagedMarkers();
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
     * Renames the node, and calls back only when it worked - {@link NodeFiles}
     * says why that needs no flag and why the cache update comes second.
     */
    public void renameNode(final @NotNull Path oldPath, final @NotNull Path newPath, final @NotNull Runnable onFinished) {
        nodeFiles.rename(oldPath, newPath, onFinished);
    }

}
