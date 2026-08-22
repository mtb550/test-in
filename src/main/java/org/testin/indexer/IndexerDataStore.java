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
import org.testin.model.dto.dirs.*;
import org.testin.model.markers.Marker;
import org.testin.model.markers.TestRunMarker;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.util.Mapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

final class IndexerDataStore {

    private final @NotNull Project p;
    private final @NotNull DirectoryChildrenIndex childrenIndex = new DirectoryChildrenIndex();
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
        this.p = p;
        this.testCaseStore = new TestCaseSequenceStore(p);
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
    private static <T> @NotNull T indexed(final @Nullable T node, final @NotNull String kind,
                                          final @NotNull Path path) {
        if (node != null) return node;

        Logger.error("No " + kind + " indexed at " + path);
        throw new IllegalStateException("No " + kind + " indexed at " + path);
    }

    void putTestCase(final @NotNull Path testSetPath, final @NotNull TestCaseDto tc) {
        testCaseStore.put(testSetPath, tc);
    }

    void putImportedTestCase(final @NotNull Path testSetPath, final @NotNull TestCaseDto tc) {
        testCaseStore.putImported(testSetPath, tc);
    }

    void removeTestCase(final @NotNull Path testSetPath, final @NotNull UUID tcId) {
        testCaseStore.remove(testSetPath, tcId);
    }

    void updateSequence(final @NotNull Path testSetPath, final @NotNull List<TestCaseDto> orderedList,
                        final @NotNull List<TestCaseDto> moved) {
        testCaseStore.updateSequence(testSetPath, orderedList, moved);
    }

    void putTestRun(final @NotNull Path testRunPath, final @NotNull TestRunDto tr) {
        registerTestRun(testRunPath, tr);

        Services.getInstance(p, FilesUtil.class)
                .write(p, testRunPath.resolve(testRunPath.getFileName() + ".json"), tr);
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

    private <V extends DirectoryDto> void addDir(final @NotNull Map<String, V> map, final @NotNull V dto,
                                                 final @NotNull String markerFileName, final @NotNull Object marker) {
        stampIfNew(marker);
        map.put(dto.getPath().toString(), dto);
        childrenIndex.invalidate();
        writeMarker(dto.getPath(), markerFileName, marker);
        refreshDir(dto.getPath());
    }

    /**
     * New markers (createdBy still blank) get the full audit stamp before
     * their first write; markers loaded from disk pass through untouched.
     */
    private void stampIfNew(final @NotNull Object marker) {
        if (marker instanceof Marker m && m.getCreatedBy().isEmpty()) {
            m.stampCreated(testerName());
        }
    }

    private @NotNull String testerName() {
        return Services.getInstance(p, AppSettingsState.class).testerName;
    }

    private void writeMarker(final @NotNull Path dirPath, final @NotNull String markerFileName, final @NotNull Object marker) {
        Services.getInstance(p, FilesUtil.class).write(p, dirPath.resolve(markerFileName), marker);
    }

    /**
     * The other half of {@link #writeMarker}, so the marker round trip is owned
     * by one class. It used to live in DirectoryMapper, which meant the indexer
     * owned the write and a mapper owned the read — the debt #49 records, which
     * grew from two markers to seven when #68 fixed the five that were written
     * and never read.
     * <p>
     * A missing or unreadable marker falls back to a default instance rather than
     * failing: the file is a type discriminator as well as a payload, so its
     * directory is a real node either way, and dropping the node out of the tree
     * would hide test cases over an unparsable audit stamp.
     */
    <M> @NotNull M readMarker(final @NotNull Path dirPath, final @NotNull String markerFileName,
                              final @NotNull Class<M> type, final @NotNull String kind, final @NotNull String name) {
        final @NotNull Path markerFile = dirPath.resolve(markerFileName);

        // Asked before reading, because a marker that is not there yet is the
        // ordinary case: a node is created, its directory appears, and the marker
        // follows. Handing an absent file to the mapper made it log an ERROR on
        // the way out - one per node created, 135 in a single sandbox session -
        // and those were the first thing a search for ERROR found. Now an ERROR
        // from the mapper means what it says: a file that is there and will not
        // parse (#66).
        if (!Files.exists(markerFile)) return defaultMarker(type, kind);

        try {
            return Services.getInstance(p, Mapper.class).readValue(markerFile.toFile(), type);

        } catch (final Exception ex) {
            Logger.warn("Unreadable " + kind + " marker '" + name + "', using defaults: " + ex.getMessage());
            return defaultMarker(type, kind);
        }
    }

    private <M> @NotNull M defaultMarker(final @NotNull Class<M> type, final @NotNull String kind) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (final Exception ex) {
            throw new RuntimeException("Cannot create default " + kind + " marker", ex);
        }
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

        Logger.info("Removed test project at: " + pathStr);
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

    void addTestProject(final @NotNull TestProjectDirectoryDto tp) {
        testProjectsByPath.put(tp.getPath().toString(), tp);
        testCasesMainDirsByPath.put(tp.getTestCasesDirectory().getPath().toString(), tp.getTestCasesDirectory());
        testRunsMainDirsByPath.put(tp.getTestRunsDirectory().getPath().toString(), tp.getTestRunsDirectory());
        childrenIndex.invalidate();

        stampIfNew(tp.getTestCasesDirectory().getMarker());
        stampIfNew(tp.getTestRunsDirectory().getMarker());
        writeMarker(tp.getTestCasesDirectory().getPath(), DirectoryType.TCD.getMarker(), tp.getTestCasesDirectory().getMarker());
        writeMarker(tp.getTestRunsDirectory().getPath(), DirectoryType.TRD.getMarker(), tp.getTestRunsDirectory().getMarker());
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
        stampIfNew(dto.getMarker());
        writeMarker(dto.getPath(), dto.getMarkerFileName(), dto.getMarker());
        childrenIndex.invalidate();
        // As every other marker write does, so the VFS - and the Git paths that
        // read through it - see the change without waiting for something else.
        refreshDir(dto.getPath());
    }

    void updateRunMarker(final @NotNull Project p, final @NotNull Path runPath, final @NotNull TestRunMarker marker) {
        Optional.ofNullable(testRunsDirByPath.get(runPath.toString()))
                .ifPresentOrElse(trd -> trd.setMarker(marker),
                        () -> Logger.warn("updateRunMarker: run dir not indexed, updating marker on disk only: " + runPath));

        Services.getInstance(p, FilesUtil.class).write(p, runPath.resolve(DirectoryType.TR.getMarker()), marker);
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
            renameMapEntry(map, oldStr, newStr, dto -> updatePathAndPath2(dto, newPath, newParentDto));
            renameDescendants(map, oldPath, newPath);
        }

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
        findByPath(newPath).ifPresent(renamed -> {
            renamed.getMarker().touch(testerName());
            writeMarker(renamed.getPath(), renamed.getMarkerFileName(), renamed.getMarker());
        });
    }

    /**
     * @param newParent null only for a node moved to a path with no parent -
     *                  the filesystem-root boundary above, carried one call
     *                  deep rather than re-derived here (#71)
     */
    private void updatePathAndPath2(final @NotNull DirectoryDto dto, final @NotNull Path newPath, final @Nullable DirectoryDto newParent) {
        dto.setPath(newPath);
        dto.setName(newPath.getFileName().toString());
        dto.setParent(newParent);
        rebuildPath2(dto);
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
            rebuildPath2(dto);
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

    private @NotNull Collection<DirectoryDto> allDirectories() {
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
