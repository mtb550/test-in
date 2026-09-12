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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.model.DirectoryType;
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

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

final class IndexerDataStore {

    private final @NotNull DirectoryChildrenIndex childrenIndex = new DirectoryChildrenIndex();

    /**
     * The marker files this index is built from, read and written in one place.
     */
    private final @NotNull MarkerFiles markers;
    private final @NotNull TestCaseSequenceStore testCaseStore;

    @Getter
    private final @NotNull Map<String, TestProjectDirectoryDto> testProjectsByPath = new ConcurrentHashMap<>();

    @Getter
    private final @NotNull Map<String, TestSetDirectoryDto> testSetsDirByPath = new ConcurrentHashMap<>();

    @Getter
    private final @NotNull Map<String, TestRunDirectoryDto> testRunsDirByPath = new ConcurrentHashMap<>();

    @Getter
    private final @NotNull Map<String, TestSetPackageDirectoryDto> testSetPackagesByPath = new ConcurrentHashMap<>();

    @Getter
    private final @NotNull Map<String, TestRunPackageDirectoryDto> testRunPackagesByPath = new ConcurrentHashMap<>();

    @Getter
    private final @NotNull Map<String, TestCasesMainDirectoryDto> testCasesMainDirsByPath = new ConcurrentHashMap<>();

    @Getter
    private final @NotNull Map<String, TestRunsMainDirectoryDto> testRunsMainDirsByPath = new ConcurrentHashMap<>();

    @Getter
    private final @NotNull Map<String, TestRunDto> testRunsByPath = new ConcurrentHashMap<>();

    /**
     * All directory maps, used by operations that must be applied uniformly
     * (rename, lookup, clear). Keep in sync when adding a new directory kind.
     */
    private final @NotNull List<Map<String, ? extends DirectoryDto>> dirMaps = List.of(
            testProjectsByPath,
            testSetsDirByPath,
            testRunsDirByPath,
            testSetPackagesByPath,
            testRunPackagesByPath,
            testCasesMainDirsByPath,
            testRunsMainDirsByPath);

    IndexerDataStore(final @NotNull Project p) {
        this.testCaseStore = new TestCaseSequenceStore(p);
        this.markers = new MarkerFiles(p);
    }

    @NotNull Map<UUID, TestCaseDto> getTestCasesById() {
        return testCaseStore.getTestCasesById();
    }

    @NotNull Map<String, List<UUID>> getTestSetCaseIds() {
        return testCaseStore.getTestSetCaseIds();
    }

    @NotNull List<TestCaseDto> getTestCasesForTestSet(final @NotNull Path testSetPath) {
        return testCaseStore.getForTestSet(testSetPath);
    }

    @NotNull
    Optional<TestRunDto> findTestRun(final @NotNull Path testRunPath) {
        return Optional.ofNullable(testRunsByPath.get(testRunPath.toString()));
    }

    @NotNull
    TestRunDto getTestRunByPath(final @NotNull Path testRunPath) {
        return indexed(testRunsByPath.get(testRunPath.toString()), "test run", testRunPath);
    }

    @NotNull
    TestRunDirectoryDto getTestRunDirByPath(final @NotNull Path path) {
        return indexed(testRunsDirByPath.get(path.toString()), "test run directory", path);
    }

    /**
     * A test case by id, which may genuinely be gone.
     * <p>
     * The one lookup here keyed by data rather than by something on screen: a
     * test run holds the ids of the cases it ran, an execution event names one,
     * and a case can be deleted after either was written. So this answers with
     * an Optional - absence is a state of the data, not a caller's mistake.
     */
    @NotNull
    Optional<TestCaseDto> findTestCase(final @NotNull UUID id) {
        return Optional.ofNullable(testCaseStore.getTestCasesById().get(id));
    }

    @NotNull
    TestSetDirectoryDto getTestSetDirByPath(final @NotNull Path path) {
        return indexed(testSetsDirByPath.get(path.toString()), "test set", path);
    }

    /**
     * A node the cache was asked for by something that already had it.
     * <p>
     * You cannot open a test set that is not indexed, rename one that is not
     * selected, or report on a run the tree is not showing - the key came out of
     * this cache, so the answer is in it. A miss is therefore a mistake in the
     * plugin, not a state of the data, and it is said once here rather than
     * guessed at by every caller.
     */
    private static <T> @NotNull T indexed(final @Nullable T node, final @NotNull String kind, final @NotNull Path path) {
        if (node != null) return node;

        Logger.error("No " + kind + " indexed at " + path);
        throw new IllegalStateException("No " + kind + " indexed at " + path);
    }

    // UC-INTERNAL-004, Rule-INTERNAL-033
    boolean putTestCase(final @NotNull Path testSetPath, final @NotNull TestCaseDto tc) {
        // The marker follows the write. A save that changed nothing did not
        // modify the set, and stamping the set's marker for it would move the
        // lie one level up (#164).
        if (!testCaseStore.put(testSetPath, tc)) return false;

        markTestSetModified(testSetPath);
        return true;
    }

    // UC-INTERNAL-004, Rule-INTERNAL-035
    void putTestCaseVerbatim(final @NotNull Path testSetPath, final @NotNull TestCaseDto tc) {
        testCaseStore.putVerbatim(testSetPath, tc);
        markTestSetModified(testSetPath);
    }

    void removeTestCase(final @NotNull Path testSetPath, final @NotNull UUID tcId) {
        testCaseStore.remove(testSetPath, tcId);
        markTestSetModified(testSetPath);
    }

    // UC-INTERNAL-004, Rule-INTERNAL-031
    void updateSequence(final @NotNull Path testSetPath, final @NotNull List<TestCaseDto> orderedList, final @NotNull List<TestCaseDto> moved) {
        testCaseStore.updateSequence(testSetPath, orderedList, moved);
        markTestSetModified(testSetPath);
    }

    /**
     * A set whose contents changed was modified, and its marker says so.
     * <p>
     * The four methods above are the four ways a set's contents change - a case
     * saved, one imported, one removed, the order rearranged - and each says so
     * here rather than each writing the marker itself. The test case already
     * carries its own audit, stamped where every save arrives; this is the other
     * half of the same fact, and without it a set edited all week reported the
     * day it was renamed.
     * <p>
     * The set only. A package and a test project are not modified because
     * something below them was: a date meaning "something, somewhere underneath"
     * cannot be read for anything, and it would write a marker per level on every
     * keystroke that saves.
     * <p>
     * Empty when the path is not an indexed test set. That is not a failure - a
     * case can be written into a set the scan has not reached yet - and it costs
     * only the marker not being touched, so it is passed over rather than raised.
     */
    private void markTestSetModified(final @NotNull Path testSetPath) {
        Optional.ofNullable(testSetsDirByPath.get(testSetPath.toString()))
                .ifPresent(ts -> markers.touched(testSetPath, DirectoryType.TS.getMarker(), ts.getMarker()));
    }

    /**
     * Index-only registration; used when the caller persists the JSON itself
     * (e.g. the run-status writer, which snapshots the bytes beforehand).
     */
    void registerTestRun(final @NotNull Path testRunPath, final @NotNull TestRunDto tr) {
        testRunsByPath.put(testRunPath.toString(), tr);
    }

    void addTestSet(final @NotNull TestSetDirectoryDto ts) {
        addDir(testSetsDirByPath, ts, DirectoryType.TS.getMarker(), ts.getMarker());
    }

    void addTestSetPackage(final @NotNull TestSetPackageDirectoryDto tsp) {
        addDir(testSetPackagesByPath, tsp, DirectoryType.TSP.getMarker(), tsp.getMarker());
    }

    void addTestRunDir(final @NotNull TestRunDirectoryDto trd) {
        addDir(testRunsDirByPath, trd, DirectoryType.TR.getMarker(), trd.getMarker());
    }

    void addTestRunPackage(final @NotNull TestRunPackageDirectoryDto trp) {
        addDir(testRunPackagesByPath, trp, DirectoryType.TRP.getMarker(), trp.getMarker());
    }

    /**
     * A new node: the marker is written first, and the cache learns about it
     * only if that landed.
     * <p>
     * Architecture rule 2, and this method used to be it inverted. Creating a
     * node performs no VFS operation of its own - the directory comes into
     * existence as a side effect of the marker write - so a cache updated first
     * left a fully indexed test set drawn in the tree with nothing on disk,
     * which then survived every rescan until the tester pressed Refresh. Now a
     * failed write reports itself and nothing is drawn (#66, finding 85).
     */
    private <V extends DirectoryDto> void addDir(final @NotNull Map<String, V> map, final @NotNull V dto, final @NotNull String markerFileName, final @NotNull Object marker) {
        if (!markers.write(dto.getPath(), markerFileName, marker)) return;

        map.put(dto.getPath().toString(), dto);
        childrenIndex.invalidate();
        refreshDir(dto.getPath());
    }

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-014.
     * <p>
     * A node's marker, read through the one class that owns both halves of that
     * file - see {@link MarkerFiles}.
     */
    <M> @NotNull M readMarker(final @NotNull Path dirPath, final @NotNull DirectoryType kind, final @NotNull Class<M> markerClass, final @NotNull String name) {
        return markers.read(dirPath, kind, markerClass, name);
    }

    /**
     * The nodes whose marker was there and would not parse, since the last time
     * anyone asked, and forgotten in the asking.
     */
    @NotNull List<String> takeDamagedMarkers() {
        return markers.takeDamaged();
    }

    /**
     * Whether a directory carries one kind's marker.
     */
    boolean hasMarker(final @NotNull Path dirPath, final @NotNull DirectoryType kind) {
        return markers.has(dirPath, kind);
    }

    /**
     * What kind a directory is marked as, asked once.
     */
    @NotNull Optional<DirectoryType> markedAs(final @NotNull Path dirPath, final @NotNull List<DirectoryType> family) {
        return markers.markedAs(dirPath, family);
    }

    /**
     * VFS refresh of a directory, off whichever thread asked for it.
     * <p>
     * The {@code async} flag of {@code refreshNioFiles} only defers the refresh
     * itself: resolving the paths to VirtualFiles happens on the calling thread
     * and reads the VFS persistence, which is a slow operation the EDT is not
     * allowed to perform. Creation flows run on the EDT, so the whole call moves
     * to a pooled thread rather than only the refresh it schedules.
     */
    void refreshDir(final @NotNull Path dirPath) {
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                LocalFileSystem.getInstance().refreshNioFiles(List.of(dirPath), true, true, null));
    }

    /**
     * Makes a single file the plugin just wrote with {@code java.nio} visible in
     * the IDE, creating its VFS entry when there is not one yet.
     * <p>
     * The directory it sits in is refreshed first, and that is the part that
     * matters: a file the VFS has never seen is discovered by re-reading its
     * parent's children, not by resolving the file itself. One level only -
     * recursion here would walk a whole project to deliver two lines of YAML,
     * which is what the directory form above is for.
     * <p>
     * Synchronous, and therefore on a pooled thread: a refresh that resolves a
     * path reads the VFS persistence, which the EDT is not allowed to do.
     */
    void refreshFile(final @NotNull Path file) {
        // Boundary: java.nio answers null for a path with no parent (#71).
        final @NotNull Optional<Path> parent = Optional.ofNullable(file.getParent());

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            parent.ifPresent(dir -> LocalFileSystem.getInstance().refreshNioFiles(List.of(dir), false, false, null));
            LocalFileSystem.getInstance().refreshAndFindFileByNioFile(file);
        });
    }

    /**
     * Drops a whole test project out of the cache: the project itself, its two
     * main directories, and every package, set and run beneath it.
     * <p>
     * Two callers, and the log line says the cache-level fact both of them mean:
     * a test project the tester deleted, and the start of a scan that is about
     * to read the same project again and must not carry the last pass's nodes
     * into this one.
     */
    void removeTestProject(final @NotNull Path path) {
        final @NotNull String pathStr = path.toString();
        testProjectsByPath.remove(pathStr);
        testCasesMainDirsByPath.entrySet().removeIf(entry -> entry.getValue().getPath().startsWith(path));
        testRunsMainDirsByPath.entrySet().removeIf(entry -> entry.getValue().getPath().startsWith(path));

        removeTestSetPackagesUnder(path);
        removeTestRunPackagesUnder(path);
        removeTestSetsUnder(path);
        removeTestRunsUnder(path);
        childrenIndex.invalidate();

        Logger.info("Test project dropped from the index: " + pathStr);
    }

    private void removeTestSetPackagesUnder(final @NotNull Path path) {
        testSetPackagesByPath.entrySet().removeIf(entry -> entry.getValue().getPath().startsWith(path));
    }

    private void removeTestRunPackagesUnder(final @NotNull Path path) {
        testRunPackagesByPath.entrySet().removeIf(entry -> entry.getValue().getPath().startsWith(path));
    }

    private void removeTestSetsUnder(final @NotNull Path path) {
        final @NotNull List<String> toRemove = testSetsDirByPath.entrySet().stream()
                .filter(entry -> entry.getValue().getPath().startsWith(path))
                .map(Map.Entry::getKey)
                .toList();
        for (final String setPath : toRemove) {
            removeTestSet(Path.of(setPath));
        }
    }

    private void removeTestRunsUnder(final @NotNull Path path) {
        final @NotNull List<String> toRemove = testRunsDirByPath.entrySet().stream()
                .filter(entry -> entry.getValue().getPath().startsWith(path))
                .map(Map.Entry::getKey)
                .toList();
        for (final String key : toRemove) {
            testRunsDirByPath.remove(key);
        }

        final @NotNull List<String> toRemoveRuns = testRunsByPath.keySet().stream()
                .filter(key -> Path.of(key).startsWith(path))
                .toList();
        for (final String key : toRemoveRuns) {
            testRunsByPath.remove(key);
        }
    }

    void removeTestSet(final @NotNull Path path) {
        final @NotNull String pathStr = path.toString();
        testSetsDirByPath.remove(pathStr);
        testCaseStore.removeForTestSet(pathStr);
        childrenIndex.invalidate();
        Logger.info("Removed test set at: " + pathStr);
    }

    void removeTestRun(final @NotNull Path path) {
        final @NotNull String pathStr = path.toString();
        testRunsDirByPath.remove(pathStr);
        testRunsByPath.remove(pathStr);
        childrenIndex.invalidate();
        Logger.info("Removed test run at: " + pathStr);
    }

    void removeTestSetPackage(final @NotNull Path path) {
        final @NotNull String pathStr = path.toString();
        testSetPackagesByPath.remove(pathStr);

        removeTestSetPackagesUnder(path);
        removeTestSetsUnder(path);
        childrenIndex.invalidate();

        Logger.info("Removed test set package at: " + pathStr);
    }

    void removeTestRunPackage(final @NotNull Path path) {
        final @NotNull String pathStr = path.toString();
        testRunPackagesByPath.remove(pathStr);

        removeTestRunPackagesUnder(path);
        removeTestRunsUnder(path);
        childrenIndex.invalidate();

        Logger.info("Removed test run package at: " + pathStr);
    }

    /**
     * A new test project, under the same order as {@link #addDir}: the two
     * markers that bring Test Cases and Test Runs into existence are written
     * first, and the cache learns about the project only if both landed.
     */
    void addTestProject(final @NotNull TestProjectDirectoryDto tp) {
        final boolean casesWritten = markers.write(tp.getTestCasesDirectory().getPath(), DirectoryType.TCD.getMarker(), tp.getTestCasesDirectory().getMarker());
        final boolean runsWritten = markers.write(tp.getTestRunsDirectory().getPath(), DirectoryType.TRD.getMarker(), tp.getTestRunsDirectory().getMarker());
        if (!casesWritten || !runsWritten) return;

        testProjectsByPath.put(tp.getPath().toString(), tp);
        testCasesMainDirsByPath.put(tp.getTestCasesDirectory().getPath().toString(), tp.getTestCasesDirectory());
        testRunsMainDirsByPath.put(tp.getTestRunsDirectory().getPath().toString(), tp.getTestRunsDirectory());
        childrenIndex.invalidate();

        refreshDir(tp.getPath());
        refreshDir(tp.getTestCasesDirectory().getPath());
        refreshDir(tp.getTestRunsDirectory().getPath());
    }

    /**
     * Writes a node's marker back after something on it changed - a status, for
     * now. The status is part of the children's sort order, so the cached lists
     * are stale the moment it is written.
     */
    void persistMarker(final @NotNull DirectoryDto dto) {
        markers.write(dto.getPath(), dto.getMarkerFileName(), dto.getMarker());
        childrenIndex.invalidate();
        // As every other marker write does, so the VFS - and the Git paths that
        // read through it - see the change without waiting for something else.
        refreshDir(dto.getPath());
    }

    void renameNode(final @NotNull Path oldPath, final @NotNull Path newPath) {
        final @NotNull String oldStr = oldPath.toString();
        final @NotNull String newStr = newPath.toString();
        // A node renamed to the top of the tree has nothing above it, which is
        // what a root is - so this stays the one nullable the model declares.
        final @Nullable DirectoryDto newParentDto = Optional.ofNullable(newPath.getParent())
                .flatMap(this::findByPath)
                .orElse(null);

        for (final Map<String, ? extends DirectoryDto> map : dirMaps) {
            renameMapEntry(map, oldStr, newStr, dto -> updatePathAndParent(dto, newPath, newParentDto));
            renameDescendants(map, oldPath, newPath);
        }

        rebuildPath2Under(newPath);

        renameMapEntry(testCaseStore.getTestSetCaseIds(), oldStr, newStr, ids -> {
        });
        renameMapEntry(testRunsByPath, oldStr, newStr, tr -> {
        });
        renameDescendantKeys(testCaseStore.getTestSetCaseIds(), oldPath, newPath);
        renameDescendantKeys(testRunsByPath, oldPath, newPath);
        childrenIndex.invalidate();

        // The renamed/moved node itself was modified - record it in the marker,
        // the persisted home of audit info. Descendants only changed location,
        // so their own audit stays untouched.
        findByPath(newPath)
                .ifPresent(renamed -> markers.touched(renamed.getPath(), renamed.getMarkerFileName(), renamed.getMarker()));
    }

    /**
     * @param newParent null only for a node moved to a path with no parent -
     *                  the filesystem-root boundary above, carried one call
     *                  deep rather than re-derived here (#71)
     */
    private void updatePathAndParent(final @NotNull DirectoryDto dto, final @NotNull Path newPath, final @Nullable DirectoryDto newParent) {
        dto.setPath(newPath);
        dto.setName(newPath.getFileName().toString());
        dto.setParent(newParent);
    }

    /**
     * Every breadcrumb at or below a moved node, rebuilt once every node above
     * it already carries its new name.
     * <p>
     * It has to be a second pass. A breadcrumb is read off the live parent
     * objects, and the seven maps are renamed in a fixed order with test sets
     * (index 1) before the packages that hold them (index 3) - so rebuilding as
     * each map was visited read the package's old name for every set beneath it.
     * Renaming a package Login to Auth left every test set under it saying
     * {@code [NAFATH, Test Cases, Login, ts2]}, and path2 is not cosmetic: the
     * code generator builds the generated Java package and class name from it,
     * and NodeRename runs the codegen rename first - so the generated subtree
     * moved to auth while the index still believed every set lived in login, and
     * the next case saved there regenerated its method into the old package
     * (#66, finding 69).
     */
    private void rebuildPath2Under(final @NotNull Path newPath) {
        allDirectories().stream()
                .filter(node -> node.getPath().startsWith(newPath))
                .forEach(this::rebuildPath2);
    }

    @NotNull
    Optional<DirectoryDto> findByPath(final @NotNull Path path) {
        final @NotNull String key = path.toString();

        return dirMaps.stream()
                .map(map -> map.get(key))
                .filter(Objects::nonNull)
                .map(DirectoryDto.class::cast)
                .findFirst();
    }

    private <V extends DirectoryDto> void renameDescendants(final @NotNull Map<String, V> map, final @NotNull Path oldPath, final @NotNull Path newPath) {
        final @NotNull List<Map.Entry<String, V>> toUpdate = new ArrayList<>();
        for (final Map.Entry<String, V> e : map.entrySet()) {
            final @NotNull Path p = e.getValue().getPath();
            if (p.startsWith(oldPath) && !p.equals(oldPath)) {
                toUpdate.add(e);
            }
        }
        for (final Map.Entry<String, V> e : toUpdate) {
            final @NotNull V dto = e.getValue();
            final @NotNull Path newChildPath = newPath.resolve(oldPath.relativize(dto.getPath()));
            map.remove(e.getKey());
            map.put(newChildPath.toString(), dto);
            dto.setPath(newChildPath);
        }
    }

    private <V> void renameDescendantKeys(final @NotNull Map<String, V> map, final @NotNull Path oldPath, final @NotNull Path newPath) {
        final @NotNull List<String> toMove = new ArrayList<>();
        for (final String key : map.keySet()) {
            final @NotNull Path p = Path.of(key);
            if (p.startsWith(oldPath) && !p.equals(oldPath)) {
                toMove.add(key);
            }
        }
        for (final String key : toMove) {
            final @NotNull V v = map.remove(key);
            final @NotNull Path newKey = newPath.resolve(oldPath.relativize(Path.of(key)));
            map.put(newKey.toString(), v);
        }
    }

    private void rebuildPath2(final @NotNull DirectoryDto dto) {
        final @NotNull ArrayList<String> path2 = new ArrayList<>();
        for (final DirectoryDto ancestor : dto.selfAndAncestors()) {
            path2.addFirst(ancestor.getName());
        }
        dto.setPath2(path2);
    }

    @NotNull List<DirectoryDto> getChildren(final @NotNull Path parentPath) {
        return childrenIndex.get(parentPath, this::allDirectories);
    }

    void invalidateChildrenIndex() {
        childrenIndex.invalidate();
    }

    /**
     * Every indexed node, of every kind, in no particular order.
     * <p>
     * Read from the seven maps the scan already fills rather than kept as an
     * eighth: a list of the same nodes would be a second record of one fact, and
     * would have to be corrected by every create, move, rename and delete that
     * the maps already handle.
     * <p>
     * Package-private rather than private since #29, which searches node names
     * and needs the same list the children index is built from - the one place
     * that already answers "every node the plugin knows".
     */
    @NotNull Collection<DirectoryDto> allDirectories() {
        // Test projects are included too; they are roots (null parent) and are
        // simply skipped by the children index.
        final @NotNull List<DirectoryDto> directories = new ArrayList<>();
        for (final Map<String, ? extends DirectoryDto> map : dirMaps) {
            directories.addAll(map.values());
        }
        return directories;
    }

    private <V> void renameMapEntry(final @NotNull Map<String, V> map, final @NotNull String oldKey, final @NotNull String newKey, final @NotNull Consumer<V> updater) {
        Optional.ofNullable(map.remove(oldKey)).ifPresent(value -> {
            updater.accept(value);
            map.put(newKey, value);
        });
    }

    void clearAll() {
        testCaseStore.clear();
        dirMaps.forEach(Map::clear);
        testRunsByPath.clear();
        childrenIndex.clear();

        Logger.info("IndexerDataStore: all maps cleared");
    }
}
