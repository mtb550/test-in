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
import org.testin.model.markers.AbstractMarker;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

final class IndexerDataStore {
    private final @NotNull DirectoryChildrenIndex childrenIndex = new DirectoryChildrenIndex();

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

    private final @NotNull List<Map<String, ? extends DirectoryDto>> dirMaps = List.of(
            testProjectsByPath,
            testSetsDirByPath,
            testRunsDirByPath,
            testSetPackagesByPath,
            testRunPackagesByPath,
            testCasesMainDirsByPath,
            testRunsMainDirsByPath);
    // Rule-INTERNAL-091
    private final @NotNull Map<String, String> refusedProjects = new ConcurrentHashMap<>();

    IndexerDataStore(final @NotNull Project p) {
        this.testCaseStore = new TestCaseSequenceStore(p);
        this.markers = new MarkerFiles(p);
    }

    private static <T> @NotNull T indexed(final @Nullable T node, final @NotNull Class<T> kind, final @NotNull Path path) {
        if (node != null) return node;

        Logger.error("No " + kind.getSimpleName() + " indexed at " + path);
        throw new IllegalStateException("No " + kind.getSimpleName() + " indexed at " + path);
    }

    private static void dropUnseen(final @NotNull Map<String, ?> held, final @NotNull Path projectPath, final @NotNull Map<String, ?> found) {
        held.keySet().removeIf(key -> Path.of(key).startsWith(projectPath) && !found.containsKey(key));
    }

    @NotNull Map<UUID, TestCaseDto> getTestCasesById() {
        return testCaseStore.getTestCasesById();
    }

    @NotNull Set<String> unreadableTestCasesIn(final @NotNull Path testSetPath) {
        return testCaseStore.unreadableIn(testSetPath);
    }

    @NotNull Path testCaseFileOf(final @NotNull TestCaseDto tc) {
        return testCaseStore.fileOf(tc.getParent().getPath(), tc.getId());
    }

    @NotNull Map<String, List<UUID>> getTestCaseIdsByTestSet() {
        return testCaseStore.getTestCaseIdsByTestSet();
    }

    @NotNull List<TestCaseDto> getTestCasesForTestSet(final @NotNull Path testSetPath) {
        return testCaseStore.getForTestSet(testSetPath);
    }

    @NotNull
    Optional<TestRunDto> findTestRun(final @NotNull Path testRunPath) {
        return Optional.ofNullable(testRunsByPath.get(testRunPath.toString()));
    }

    @NotNull Optional<TestRunDirectoryDto> findTestRunDir(final @NotNull Path testRunPath) {
        return Optional.ofNullable(testRunsDirByPath.get(testRunPath.toString()));
    }

    @NotNull
    TestRunDto getTestRunByPath(final @NotNull Path testRunPath) {
        return indexed(testRunsByPath.get(testRunPath.toString()), TestRunDto.class, testRunPath);
    }

    @NotNull
    Optional<TestCaseDto> findTestCase(final @NotNull UUID id) {
        return Optional.ofNullable(testCaseStore.getTestCasesById().get(id));
    }

    @NotNull
    TestSetDirectoryDto getTestSetDirByPath(final @NotNull Path path) {
        return indexed(testSetsDirByPath.get(path.toString()), TestSetDirectoryDto.class, path);
    }

    // UC-INTERNAL-004, Rule-INTERNAL-033
    boolean putTestCase(final @NotNull Path testSetPath, final @NotNull TestCaseDto tc) {
        if (!testCaseStore.put(testSetPath, tc)) return false;

        markTestSetModified(testSetPath);
        return true;
    }

    // UC-INTERNAL-004, Rule-INTERNAL-035
    boolean putTestCaseVerbatim(final @NotNull Path testSetPath, final @NotNull TestCaseDto tc) {
        if (!testCaseStore.putVerbatim(testSetPath, tc)) return false;

        markTestSetModified(testSetPath);
        return true;
    }

    // UC-EDITOR-PANEL-017, Rule-INTERNAL-035
    boolean moveTestCase(final @NotNull Path fromSet, final @NotNull Path toSet, final @NotNull TestCaseDto tc) {
        if (!testCaseStore.move(fromSet, toSet, tc)) return false;

        markTestSetModified(toSet);
        if (!fromSet.equals(toSet)) markTestSetModified(fromSet);
        return true;
    }

    boolean removeTestCase(final @NotNull Path testSetPath, final @NotNull UUID tcId) {
        if (!testCaseStore.remove(testSetPath, tcId)) return false;

        markTestSetModified(testSetPath);
        return true;
    }

    // UC-INTERNAL-004, Rule-INTERNAL-031
    void updateSequence(final @NotNull Path testSetPath, final @NotNull List<TestCaseDto> orderedList, final @NotNull List<TestCaseDto> moved) {
        testCaseStore.updateSequence(testSetPath, orderedList, moved);
        markTestSetModified(testSetPath);
    }

    private void markTestSetModified(final @NotNull Path testSetPath) {
        Optional.ofNullable(testSetsDirByPath.get(testSetPath.toString()))
                .ifPresent(ts -> markers.touched(testSetPath, DirectoryType.TS.getMarker(), ts.getMarker()));
    }

    void registerTestRun(final @NotNull Path testRunPath, final @NotNull TestRunDto tr) {
        testRunsByPath.put(testRunPath.toString(), tr);
    }

    boolean addTestSet(final @NotNull TestSetDirectoryDto ts) {
        return addDir(testSetsDirByPath, ts, DirectoryType.TS.getMarker(), ts.getMarker());
    }

    boolean addTestSetPackage(final @NotNull TestSetPackageDirectoryDto tsp) {
        return addDir(testSetPackagesByPath, tsp, DirectoryType.TSP.getMarker(), tsp.getMarker());
    }

    boolean addTestRunDir(final @NotNull TestRunDirectoryDto trd) {
        return addDir(testRunsDirByPath, trd, DirectoryType.TR.getMarker(), trd.getMarker());
    }

    boolean addTestRunPackage(final @NotNull TestRunPackageDirectoryDto trp) {
        return addDir(testRunPackagesByPath, trp, DirectoryType.TRP.getMarker(), trp.getMarker());
    }

    private <V extends DirectoryDto> boolean addDir(final @NotNull Map<String, V> map, final @NotNull V dto, final @NotNull String markerFileName, final @NotNull Object marker) {
        if (!markers.write(dto.getPath(), markerFileName, marker)) return false;

        map.put(dto.getPath().toString(), dto);
        childrenIndex.invalidate();
        refreshDir(dto.getPath());
        return true;
    }

    // UC-INTERNAL-002, Rule-INTERNAL-014
    <M extends AbstractMarker> @NotNull M readMarker(final @NotNull Path dirPath, final @NotNull DirectoryType kind, final @NotNull String name, final @NotNull Class<M> markerClass) {
        return markers.read(dirPath, kind, name, markerClass);
    }

    @NotNull List<String> takeDamagedMarkers() {
        return markers.takeDamaged();
    }

    void refuse(final @NotNull Path projectPath, final @NotNull String reason) {
        refusedProjects.put(projectPath.toString(), reason);
    }

    void readable(final @NotNull Path projectPath) {
        refusedProjects.remove(projectPath.toString());
    }

    @NotNull Optional<String> whyNotRead(final @NotNull Path projectPath) {
        return Optional.ofNullable(refusedProjects.get(projectPath.toString()));
    }

    // Rule-INTERNAL-090, Rule-TREE-PANEL-051
    boolean giveFreshMarkerId(final @NotNull Path markerFile) {
        return markers.giveFreshId(markerFile);
    }

    boolean hasMarker(final @NotNull Path dirPath, final @NotNull DirectoryType kind) {
        return markers.has(dirPath, kind);
    }

    @NotNull Optional<DirectoryType> markedAs(final @NotNull Path dirPath, final @NotNull List<DirectoryType> family) {
        return markers.markedAs(dirPath, family);
    }

    void refreshDir(final @NotNull Path dirPath) {
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                LocalFileSystem.getInstance().refreshNioFiles(List.of(dirPath), true, true, null));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-021
    void swapIn(final @NotNull Path projectPath, final @NotNull ScannedProject scanned) {
        testProjectsByPath.putAll(scanned.getProjects());
        testCasesMainDirsByPath.putAll(scanned.getTestCasesMainDirs());
        testRunsMainDirsByPath.putAll(scanned.getTestRunsMainDirs());
        testSetPackagesByPath.putAll(scanned.getTestSetPackages());
        testRunPackagesByPath.putAll(scanned.getTestRunPackages());
        testSetsDirByPath.putAll(scanned.getTestSets());
        testRunsDirByPath.putAll(scanned.getTestRunDirs());
        testRunsByPath.putAll(scanned.getTestRuns());

        testCaseStore.swapIn(projectPath, scanned.getTestCasesById(), scanned.getTestCaseIdsByTestSet(), scanned.handNamedFilesAlone(), scanned.getUnreadableTestCases());

        dropUnseen(testProjectsByPath, projectPath, scanned.getProjects());
        dropUnseen(testCasesMainDirsByPath, projectPath, scanned.getTestCasesMainDirs());
        dropUnseen(testRunsMainDirsByPath, projectPath, scanned.getTestRunsMainDirs());
        dropUnseen(testSetPackagesByPath, projectPath, scanned.getTestSetPackages());
        dropUnseen(testRunPackagesByPath, projectPath, scanned.getTestRunPackages());
        dropUnseen(testSetsDirByPath, projectPath, scanned.getTestSets());
        dropUnseen(testRunsDirByPath, projectPath, scanned.getTestRunDirs());
        dropUnseen(testRunsByPath, projectPath, scanned.getTestRuns());

        childrenIndex.invalidate();
    }

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

    boolean addTestProject(final @NotNull TestProjectDirectoryDto tp) {
        final boolean written = markers.write(tp.getPath(), tp.getMarkerFileName(), tp.getMarker())
                && markers.write(tp.getTestCasesDirectory().getPath(), DirectoryType.TCD.getMarker(), tp.getTestCasesDirectory().getMarker())
                && markers.write(tp.getTestRunsDirectory().getPath(), DirectoryType.TRD.getMarker(), tp.getTestRunsDirectory().getMarker());
        if (!written) return false;

        testProjectsByPath.put(tp.getPath().toString(), tp);
        testCasesMainDirsByPath.put(tp.getTestCasesDirectory().getPath().toString(), tp.getTestCasesDirectory());
        testRunsMainDirsByPath.put(tp.getTestRunsDirectory().getPath().toString(), tp.getTestRunsDirectory());
        childrenIndex.invalidate();

        refreshDir(tp.getPath());
        refreshDir(tp.getTestCasesDirectory().getPath());
        refreshDir(tp.getTestRunsDirectory().getPath());
        return true;
    }

    boolean persistMarker(final @NotNull DirectoryDto dto) {
        final boolean written = markers.write(dto.getPath(), dto.getMarkerFileName(), dto.getMarker());
        childrenIndex.invalidate();
        refreshDir(dto.getPath());
        return written;
    }

    // Rule-INTERNAL-083, Rule-INTERNAL-090
    boolean persistRunMarker(final @NotNull Path runPath) {
        return findTestRunDir(runPath).map(this::persistMarker).orElse(false);
    }

    void renameNode(final @NotNull Path oldPath, final @NotNull Path newPath) {
        final @NotNull String oldStr = oldPath.toString();
        final @NotNull String newStr = newPath.toString();
        final @Nullable DirectoryDto newParentDto = Optional.ofNullable(newPath.getParent())
                .flatMap(this::findByPath)
                .orElse(null);

        for (final Map<String, ? extends DirectoryDto> map : dirMaps) {
            renameMapEntry(map, oldStr, newStr, dto -> updatePathAndParent(dto, newPath, newParentDto));
            renameDescendants(map, oldPath, newPath);
        }

        rebuildPath2Under(newPath);

        renameMapEntry(testCaseStore.getTestCaseIdsByTestSet(), oldStr, newStr, _ -> {
        });
        renameMapEntry(testRunsByPath, oldStr, newStr, _ -> {
        });
        renameDescendantKeys(testCaseStore.getTestCaseIdsByTestSet(), oldPath, newPath);
        renameDescendantKeys(testRunsByPath, oldPath, newPath);
        testCaseStore.renamed(oldPath, newPath);
        childrenIndex.invalidate();

        // UC-TREE-PANEL-011, Rule-TREE-PANEL-100
        findByPath(newPath).ifPresent(renamed -> renamed.fixedChildren().forEach(child ->
                updatePathAndParent(child, newPath.resolve(child.getPath().getFileName()), renamed)));

        findByPath(newPath)
                .ifPresent(renamed -> markers.touched(renamed.getPath(), renamed.getMarkerFileName(), renamed.getMarker()));
    }

    private void updatePathAndParent(final @NotNull DirectoryDto dto, final @NotNull Path newPath, final @Nullable DirectoryDto newParent) {
        dto.setPath(newPath);
        dto.setName(newPath.getFileName().toString());
        dto.setParent(newParent);
    }

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

    @NotNull Collection<DirectoryDto> allDirectories() {
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
