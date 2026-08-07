package org.testin.indexer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.DirectoryType;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.TestRunDto;
import org.testin.mappers.dto.dirs.*;
import org.testin.mappers.markers.TestRunMarker;
import org.testin.services.Services;
import org.testin.util.FilesUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class IndexerDataStore {

    private final @NotNull Project p;

    @Getter
    private final Map<UUID, TestCaseDto> testCasesById = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, TestProjectDirectoryDto> testProjectsByPath = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, TestSetDirectoryDto> testSetsDirByPath = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, TestRunDirectoryDto> testRunsDirByPath = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, TestSetPackageDirectoryDto> testSetPackagesByPath = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, TestRunPackageDirectoryDto> testRunPackagesByPath = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, TestCasesMainDirectoryDto> testCasesMainDirsByPath = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, TestRunsMainDirectoryDto> testRunsMainDirsByPath = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, List<UUID>> testSetCaseIds = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, TestRunDto> testRunsByPath = new ConcurrentHashMap<>();

    IndexerDataStore(final @NotNull Project p) {
        this.p = p;
    }

    List<TestCaseDto> getTestCasesForTestSet(final Path testSetPath) {
        final List<UUID> ids = testSetCaseIds.get(testSetPath.toString());
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        final Map<UUID, TestCaseDto> byId = new HashMap<>(ids.size());
        for (final UUID id : ids) {
            final TestCaseDto tc = testCasesById.get(id);
            if (tc != null) byId.put(id, tc);
        }

        final List<TestCaseDto> result = new ArrayList<>(byId.size());
        final Set<UUID> visited = new HashSet<>();

        TestCaseDto head = null;
        for (final TestCaseDto tc : byId.values()) {
            if (Boolean.TRUE.equals(tc.getIsHead())) {
                head = tc;
                break;
            }
        }

        if (head != null) {
            TestCaseDto current = head;
            while (current != null && !visited.contains(current.getId())) {
                result.add(current);
                visited.add(current.getId());
                final UUID nextUuid = current.getNext();
                current = (nextUuid != null) ? byId.get(nextUuid) : null;
            }
        }

        for (final UUID id : ids) {
            final TestCaseDto tc = testCasesById.get(id);
            if (tc != null && !visited.contains(tc.getId())) {
                result.add(tc);
                visited.add(tc.getId());
            }
        }

        return result;
    }

    @org.jetbrains.annotations.Nullable
    TestRunDto getTestRunByPath(final Path testRunPath) {
        return testRunsByPath.get(testRunPath.toString());
    }

    @org.jetbrains.annotations.Nullable
    TestRunDirectoryDto getTestRunDirByPath(final Path path) {
        return testRunsDirByPath.get(path.toString());
    }

    @org.jetbrains.annotations.Nullable
    TestCaseDto getTestCaseById(final UUID id) {
        return testCasesById.get(id);
    }

    @org.jetbrains.annotations.Nullable
    TestSetDirectoryDto getTestSetDirByPath(final Path path) {
        return testSetsDirByPath.get(path.toString());
    }

    @org.jetbrains.annotations.Nullable
    TestSetPackageDirectoryDto getTestSetPackageByPath(final Path path) {
        return testSetPackagesByPath.get(path.toString());
    }

    void putTestCase(final Path testSetPath, final TestCaseDto tc) {
        final String pathStr = testSetPath.toString();
        testCasesById.put(tc.getId(), tc);

        final List<UUID> ids = testSetCaseIds.computeIfAbsent(
                pathStr, k -> Collections.synchronizedList(new ArrayList<>()));
        if (!ids.contains(tc.getId())) {
            ids.add(tc.getId());
        }

        Services.getInstance(p, FilesUtil.class)
                .write(p, testSetPath.resolve(tc.getId() + ".json"), tc);
    }

    void removeTestCase(final Path testSetPath, final UUID tcId) {
        final String pathStr = testSetPath.toString();
        testCasesById.remove(tcId);

        final List<UUID> ids = testSetCaseIds.get(pathStr);
        if (ids != null) ids.remove(tcId);

        final Path filePath = testSetPath.resolve(tcId + ".json");
        try {
            Files.deleteIfExists(filePath);
        } catch (final Exception ex) {
            Logger.error("Failed to delete test case file: " + filePath);
        }
    }

    void updateSequence(final Path testSetPath, final List<TestCaseDto> sortedList) {
        final String pathStr = testSetPath.toString();
        final List<UUID> ids = new ArrayList<>(sortedList.size());
        final Set<UUID> newIds = new HashSet<>();

        for (int i = 0; i < sortedList.size(); i++) {
            final TestCaseDto tc = sortedList.get(i);
            tc.setIsHead(i == 0);
            tc.setNext(i < sortedList.size() - 1 ? sortedList.get(i + 1).getId() : null);
            ids.add(tc.getId());
            newIds.add(tc.getId());
            testCasesById.put(tc.getId(), tc);

            Services.getInstance(p, FilesUtil.class)
                    .write(p, testSetPath.resolve(tc.getId() + ".json"), tc);
        }

        final List<UUID> oldIds = testSetCaseIds.get(pathStr);
        if (oldIds != null) {
            for (final UUID oldId : oldIds) {
                if (!newIds.contains(oldId)) {
                    testCasesById.remove(oldId);
                }
            }
        }

        testSetCaseIds.put(pathStr, ids);
    }

    void putTestRun(final Path testRunPath, final TestRunDto tr) {
        testRunsByPath.put(testRunPath.toString(), tr);

        Services.getInstance(p, FilesUtil.class)
                .write(p, testRunPath.resolve(testRunPath.getFileName() + ".json"), tr);
    }

    void addTestSet(final TestSetDirectoryDto ts) {
        testSetsDirByPath.put(ts.getPath().toString(), ts);
        writeMarker(ts.getPath(), DirectoryType.TS.getMarker(), ts.getMarker());
        refreshDir(ts.getPath());
    }

    void addTestSetPackage(final TestSetPackageDirectoryDto tsp) {
        testSetPackagesByPath.put(tsp.getPath().toString(), tsp);
        writeMarker(tsp.getPath(), DirectoryType.TSP.getMarker(), tsp.getMarker());
        refreshDir(tsp.getPath());
    }

    void addTestRunDir(final @NotNull TestRunDirectoryDto trd) {
        testRunsDirByPath.put(trd.getPath().toString(), trd);
        writeMarker(trd.getPath(), DirectoryType.TR.getMarker(), trd.getMarker());
        refreshDir(trd.getPath());
    }

    void addTestRunPackage(final @NotNull TestRunPackageDirectoryDto trp) {
        testRunPackagesByPath.put(trp.getPath().toString(), trp);
        writeMarker(trp.getPath(), DirectoryType.TRP.getMarker(), trp.getMarker());
        refreshDir(trp.getPath());
    }

    private void writeMarker(final @NotNull Path dirPath, final @NotNull String markerFileName, final @NotNull Object marker) {
        Services.getInstance(p, FilesUtil.class).write(p, dirPath.resolve(markerFileName), marker);
    }

    private void refreshDir(final @NotNull Path dirPath) {
        ApplicationManager.getApplication().invokeLater(() -> WriteAction.run(() -> {
            final VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(dirPath);
            if (vf != null) {
                vf.refresh(false, true);
            }
        }));
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

        final List<UUID> ids = testSetCaseIds.remove(pathStr);
        if (ids != null) {
            for (final UUID id : ids) {
                testCasesById.remove(id);
            }
        }
        Logger.info("Removed test set at: " + pathStr);
    }

    void removeTestRun(final @NotNull Path path) {
        final String pathStr = path.toString();
        testRunsDirByPath.remove(pathStr);
        testRunsByPath.remove(pathStr);
        Logger.info("Removed test run at: " + pathStr);
    }

    void removeTestSetPackage(final @NotNull Path path) {
        final String pathStr = path.toString();
        testSetPackagesByPath.remove(pathStr);

        removeTestSetPackagesUnder(path);
        removeTestSetsUnder(path);

        Logger.info("Removed test set package at: " + pathStr);
    }

    void removeTestRunPackage(final @NotNull Path path) {
        final String pathStr = path.toString();
        testRunPackagesByPath.remove(pathStr);

        removeTestRunPackagesUnder(path);
        removeTestRunsUnder(path);

        Logger.info("Removed test run package at: " + pathStr);
    }

    void addTestProject(final @NotNull TestProjectDirectoryDto tp) {
        testProjectsByPath.put(tp.getPath().toString(), tp);
        testCasesMainDirsByPath.put(tp.getTestCasesDirectory().getPath().toString(), tp.getTestCasesDirectory());
        testRunsMainDirsByPath.put(tp.getTestRunsDirectory().getPath().toString(), tp.getTestRunsDirectory());

        writeMarker(tp.getTestCasesDirectory().getPath(), DirectoryType.TCD.getMarker(), tp.getTestCasesDirectory().getMarker());
        writeMarker(tp.getTestRunsDirectory().getPath(), DirectoryType.TRD.getMarker(), tp.getTestRunsDirectory().getMarker());
        refreshDir(tp.getPath());
        refreshDir(tp.getTestCasesDirectory().getPath());
        refreshDir(tp.getTestRunsDirectory().getPath());
    }

    void addTestProjectMarker(final @NotNull Project p, final @NotNull TestProjectDirectoryDto tp) {
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

        renameMapEntry(testProjectsByPath, oldStr, newStr, dto -> updatePathAndPath2(dto, newPath));
        renameMapEntry(testSetsDirByPath, oldStr, newStr, dto -> updatePathAndPath2(dto, newPath));
        renameMapEntry(testRunsDirByPath, oldStr, newStr, dto -> updatePathAndPath2(dto, newPath));
        renameMapEntry(testSetPackagesByPath, oldStr, newStr, dto -> updatePathAndPath2(dto, newPath));
        renameMapEntry(testRunPackagesByPath, oldStr, newStr, dto -> updatePathAndPath2(dto, newPath));
        renameMapEntry(testCasesMainDirsByPath, oldStr, newStr, dto -> updatePathAndPath2(dto, newPath));
        renameMapEntry(testRunsMainDirsByPath, oldStr, newStr, dto -> updatePathAndPath2(dto, newPath));
        renameMapEntry(testSetCaseIds, oldStr, newStr, ids -> {
        });
        renameMapEntry(testRunsByPath, oldStr, newStr, tr -> {
        });

        renameDescendants(testProjectsByPath, oldPath, newPath);
        renameDescendants(testSetsDirByPath, oldPath, newPath);
        renameDescendants(testRunsDirByPath, oldPath, newPath);
        renameDescendants(testSetPackagesByPath, oldPath, newPath);
        renameDescendants(testRunPackagesByPath, oldPath, newPath);
        renameDescendants(testCasesMainDirsByPath, oldPath, newPath);
        renameDescendants(testRunsMainDirsByPath, oldPath, newPath);
        renameDescendantKeys(testSetCaseIds, oldPath, newPath);
        renameDescendantKeys(testRunsByPath, oldPath, newPath);
    }

    private void updatePathAndPath2(final @NotNull DirectoryDto dto, final Path newPath) {
        dto.setPath(newPath);
        dto.setName(newPath.getFileName().toString());
        dto.setModifiedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        dto.setModifiedBy(System.getProperty("user.name", ""));
        rebuildPath2(dto);
    }

    private <V extends DirectoryDto> void renameDescendants(final Map<String, V> map, final Path oldPath, final Path newPath) {
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

    private <V> void renameDescendantKeys(final Map<String, V> map, final Path oldPath, final Path newPath) {
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

    List<DirectoryDto> getChildren(final Path parentPath) {
        final String parentStr = parentPath.toString();
        final List<DirectoryDto> children = new ArrayList<>();

        for (final TestCasesMainDirectoryDto dto : testCasesMainDirsByPath.values()) {
            if (dto.getParent() != null && dto.getParent().getPath().toString().equals(parentStr)) {
                children.add(dto);
            }
        }
        for (final TestRunsMainDirectoryDto dto : testRunsMainDirsByPath.values()) {
            if (dto.getParent() != null && dto.getParent().getPath().toString().equals(parentStr)) {
                children.add(dto);
            }
        }
        for (final TestSetPackageDirectoryDto dto : testSetPackagesByPath.values()) {
            if (dto.getParent() != null && dto.getParent().getPath().toString().equals(parentStr)) {
                children.add(dto);
            }
        }
        for (final TestSetDirectoryDto dto : testSetsDirByPath.values()) {
            if (dto.getParent() != null && dto.getParent().getPath().toString().equals(parentStr)) {
                children.add(dto);
            }
        }
        for (final TestRunPackageDirectoryDto dto : testRunPackagesByPath.values()) {
            if (dto.getParent() != null && dto.getParent().getPath().toString().equals(parentStr)) {
                children.add(dto);
            }
        }
        for (final TestRunDirectoryDto dto : testRunsDirByPath.values()) {
            if (dto.getParent() != null && dto.getParent().getPath().toString().equals(parentStr)) {
                children.add(dto);
            }
        }

        children.sort(Comparator.comparing(DirectoryDto::getName));
        return children;
    }

    private <V> void renameMapEntry(final Map<String, V> map, final String oldKey,
                                    final String newKey, final java.util.function.Consumer<V> updater) {
        final V value = map.remove(oldKey);
        if (value != null) {
            updater.accept(value);
            map.put(newKey, value);
        }
    }


    void clearAll() {
        testCasesById.clear();
        testProjectsByPath.clear();
        testSetsDirByPath.clear();
        testRunsDirByPath.clear();
        testSetPackagesByPath.clear();
        testRunPackagesByPath.clear();
        testCasesMainDirsByPath.clear();
        testRunsMainDirsByPath.clear();
        testSetCaseIds.clear();
        testRunsByPath.clear();

        Logger.info("IndexerDataStore: all maps cleared");
    }
}
