package org.testin.indexer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.DirectoryType;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.*;
import org.testin.model.markers.Marker;
import org.testin.model.markers.TestRunMarker;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.util.FilesUtil;

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

    @Nullable
    TestRunDto getTestRunByPath(final @NotNull Path testRunPath) {
        return testRunsByPath.get(testRunPath.toString());
    }

    @Nullable
    TestRunDirectoryDto getTestRunDirByPath(final @NotNull Path path) {
        return testRunsDirByPath.get(path.toString());
    }

    @Nullable
    TestCaseDto getTestCaseById(final @NotNull UUID id) {
        return testCaseStore.getTestCasesById().get(id);
    }

    @Nullable
    TestSetDirectoryDto getTestSetDirByPath(final @NotNull Path path) {
        return testSetsDirByPath.get(path.toString());
    }

    void putTestCase(final @NotNull Path testSetPath, final @NotNull TestCaseDto tc) {
        testCaseStore.put(testSetPath, tc);
    }

    void removeTestCase(final @NotNull Path testSetPath, final @NotNull UUID tcId) {
        testCaseStore.remove(testSetPath, tcId);
    }

    void updateSequence(final @NotNull Path testSetPath, final @NotNull List<TestCaseDto> sortedList) {
        testCaseStore.updateSequence(testSetPath, sortedList);
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

    void removeTestProject(final @NotNull Path path) {
        final String pathStr = path.toString();
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
        final List<String> toRemove = testSetsDirByPath.entrySet().stream()
                .filter(entry -> entry.getValue().getPath().startsWith(path))
                .map(Map.Entry::getKey)
                .toList();
        for (final String setPath : toRemove) {
            removeTestSet(Path.of(setPath));
        }
    }

    private void removeTestRunsUnder(final @NotNull Path path) {
        final List<String> toRemove = testRunsDirByPath.entrySet().stream()
                .filter(entry -> entry.getValue().getPath().startsWith(path))
                .map(Map.Entry::getKey)
                .toList();
        for (final String key : toRemove) {
            testRunsDirByPath.remove(key);
        }

        final List<String> toRemoveRuns = testRunsByPath.keySet().stream()
                .filter(key -> Path.of(key).startsWith(path))
                .toList();
        for (final String key : toRemoveRuns) {
            testRunsByPath.remove(key);
        }
    }

    void removeTestSet(final @NotNull Path path) {
        final String pathStr = path.toString();
        testSetsDirByPath.remove(pathStr);
        testCaseStore.removeForTestSet(pathStr);
        childrenIndex.invalidate();
        Logger.info("Removed test set at: " + pathStr);
    }

    void removeTestRun(final @NotNull Path path) {
        final String pathStr = path.toString();
        testRunsDirByPath.remove(pathStr);
        testRunsByPath.remove(pathStr);
        childrenIndex.invalidate();
        Logger.info("Removed test run at: " + pathStr);
    }

    void removeTestSetPackage(final @NotNull Path path) {
        final String pathStr = path.toString();
        testSetPackagesByPath.remove(pathStr);

        removeTestSetPackagesUnder(path);
        removeTestSetsUnder(path);
        childrenIndex.invalidate();

        Logger.info("Removed test set package at: " + pathStr);
    }

    void removeTestRunPackage(final @NotNull Path path) {
        final String pathStr = path.toString();
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

    void addTestProjectMarker(final @NotNull Project p, final @NotNull TestProjectDirectoryDto tp) {
        stampIfNew(tp.getMarker());
        final Path markerPath = tp.getPath().resolve(DirectoryType.TP.getMarker());
        Services.getInstance(p, FilesUtil.class).write(p, markerPath, tp.getMarker());
    }

    void updateRunMarker(final @NotNull Project p, final @NotNull Path runPath, final @NotNull TestRunMarker marker) {
        final TestRunDirectoryDto trd = testRunsDirByPath.get(runPath.toString());
        if (trd != null) {
            trd.setMarker(marker);
        } else {
            Logger.warn("updateRunMarker: run dir not indexed, updating marker on disk only: " + runPath);
        }

        Services.getInstance(p, FilesUtil.class).write(p, runPath.resolve(DirectoryType.TR.getMarker()), marker);
    }

    void renameNode(final @NotNull Path oldPath, final @NotNull Path newPath) {
        final String oldStr = oldPath.toString();
        final String newStr = newPath.toString();
        final Path newParentPath = newPath.getParent();
        final DirectoryDto newParentDto = newParentPath == null ? null : findByPath(newParentPath);

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
        final DirectoryDto renamed = findByPath(newPath);
        if (renamed != null) {
            renamed.getMarker().touch(testerName());
            writeMarker(renamed.getPath(), renamed.getMarkerFileName(), renamed.getMarker());
        }
    }

    private void updatePathAndPath2(final @NotNull DirectoryDto dto, final @NotNull Path newPath, final @Nullable DirectoryDto newParent) {
        dto.setPath(newPath);
        dto.setName(newPath.getFileName().toString());
        dto.setParent(newParent);
        rebuildPath2(dto);
    }

    @Nullable
    DirectoryDto findByPath(final @NotNull Path path) {
        final String key = path.toString();
        for (final Map<String, ? extends DirectoryDto> map : dirMaps) {
            final DirectoryDto dto = map.get(key);
            if (dto != null) return dto;
        }
        return null;
    }

    private <V extends DirectoryDto> void renameDescendants(final @NotNull Map<String, V> map, final @NotNull Path oldPath, final @NotNull Path newPath) {
        final List<Map.Entry<String, V>> toUpdate = new ArrayList<>();
        for (final Map.Entry<String, V> e : map.entrySet()) {
            final Path p = e.getValue().getPath();
            if (p.startsWith(oldPath) && !p.equals(oldPath)) {
                toUpdate.add(e);
            }
        }
        for (final Map.Entry<String, V> e : toUpdate) {
            final V dto = e.getValue();
            final Path newChildPath = newPath.resolve(oldPath.relativize(dto.getPath()));
            map.remove(e.getKey());
            map.put(newChildPath.toString(), dto);
            dto.setPath(newChildPath);
            rebuildPath2(dto);
        }
    }

    private <V> void renameDescendantKeys(final @NotNull Map<String, V> map, final @NotNull Path oldPath, final @NotNull Path newPath) {
        final List<String> toMove = new ArrayList<>();
        for (final String key : map.keySet()) {
            final Path p = Path.of(key);
            if (p.startsWith(oldPath) && !p.equals(oldPath)) {
                toMove.add(key);
            }
        }
        for (final String key : toMove) {
            final V v = map.remove(key);
            final Path newKey = newPath.resolve(oldPath.relativize(Path.of(key)));
            map.put(newKey.toString(), v);
        }
    }

    private void rebuildPath2(final @NotNull DirectoryDto dto) {
        final ArrayList<String> path2 = new ArrayList<>();
        for (DirectoryDto cur = dto; cur != null; cur = cur.getParent()) {
            path2.addFirst(cur.getName());
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
        final List<DirectoryDto> directories = new ArrayList<>();
        for (final Map<String, ? extends DirectoryDto> map : dirMaps) {
            directories.addAll(map.values());
        }
        return directories;
    }

    private <V> void renameMapEntry(final @NotNull Map<String, V> map, final @NotNull String oldKey, final @NotNull String newKey, final @NotNull Consumer<V> updater) {
        final V value = map.remove(oldKey);
        if (value != null) {
            updater.accept(value);
            map.put(newKey, value);
        }
    }

    void clearAll() {
        testCaseStore.clear();
        dirMaps.forEach(Map::clear);
        testRunsByPath.clear();
        childrenIndex.clear();

        Logger.info("IndexerDataStore: all maps cleared");
    }
}
