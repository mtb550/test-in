package org.testin.util.indexer;

import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.DirectoryType;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.TestRunDto;
import org.testin.mappers.dto.dirs.*;
import org.testin.mappers.markers.TestRunMarker;
import org.testin.util.FilesUtil;
import org.testin.util.Tools;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class IndexerDataStore {

    private final Project project;

    @Getter
    private final Map<UUID, TestCaseDto> testCasesById = new ConcurrentHashMap<>();

    @Getter
    private final Map<UUID, TestRunDto> testRunsById = new ConcurrentHashMap<>();

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

    IndexerDataStore(final @NotNull Project project) {
        this.project = project;
    }

    List<TestCaseDto> getTestCasesForTestSet(final Path testSetPath) {
        final List<UUID> ids = testSetCaseIds.get(testSetPath.toString());
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        // Build a per-set id -> dto map so we can walk the linked list (next/isHead)
        // exactly as it was authored, instead of relying on global-map iteration order.
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

        // Fallback: append any case not reachable from the head so a set is never
        // silently truncated when the linked-list is incomplete/broken.
        for (final UUID id : ids) {
            final TestCaseDto tc = testCasesById.get(id);
            if (tc != null && !visited.contains(tc.getId())) {
                result.add(tc);
                visited.add(tc.getId());
            }
        }

        return result;
    }

    TestRunDto getTestRunByPath(final Path testRunPath) {
        return testRunsByPath.get(testRunPath.toString());
    }

    TestRunDirectoryDto getTestRunDirByPath(final Path path) {
        return testRunsDirByPath.get(path.toString());
    }

    TestCaseDto getTestCaseById(final UUID id) {
        return testCasesById.get(id);
    }

    TestSetDirectoryDto getTestSetDirByPath(final Path path) {
        return testSetsDirByPath.get(path.toString());
    }

    TestSetPackageDirectoryDto getTestSetPackageByPath(final Path path) {
        return testSetPackagesByPath.get(path.toString());
    }

    void putTestCase(final Path testSetPath, final TestCaseDto tc) {
        testCasesById.put(tc.getId(), tc);

        final List<UUID> ids = testSetCaseIds.computeIfAbsent(
                testSetPath.toString(), k -> Collections.synchronizedList(new ArrayList<>()));
        if (!ids.contains(tc.getId())) {
            ids.add(tc.getId());
        }

        Services.getInstance(project, FilesUtil.class)
                .write(project, testSetPath.resolve(tc.getId() + ".json"), tc);
    }

    void removeTestCase(final Path testSetPath, final UUID tcId) {
        testCasesById.remove(tcId);

        final List<UUID> ids = testSetCaseIds.get(testSetPath.toString());
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

            Services.getInstance(project, FilesUtil.class)
                    .write(project, testSetPath.resolve(tc.getId() + ".json"), tc);
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

        Services.getInstance(project, FilesUtil.class)
                .write(project, testRunPath.resolve(testRunPath.getFileName() + ".json"), tr);
    }

    void addTestSet(final TestSetDirectoryDto ts) {
        testSetsDirByPath.put(ts.getPath().toString(), ts);
    }

    void addTestSetPackage(final TestSetPackageDirectoryDto tsp) {
        testSetPackagesByPath.put(tsp.getPath().toString(), tsp);
    }

    void addTestRunDir(final @NotNull TestRunDirectoryDto trd) {
        testRunsDirByPath.put(trd.getPath().toString(), trd);
    }

    void addTestRunPackage(final @NotNull TestRunPackageDirectoryDto trp) {
        testRunPackagesByPath.put(trp.getPath().toString(), trp);
    }

    void removeTestProject(final @NotNull Path path) {
        final String pathStr = path.toString();
        testProjectsByPath.remove(pathStr);
        testCasesMainDirsByPath.entrySet().removeIf(entry -> entry.getValue().getPath().startsWith(path));
        testRunsMainDirsByPath.entrySet().removeIf(entry -> entry.getValue().getPath().startsWith(path));
        Logger.info("Removed test project at: " + pathStr);
    }

    void removeTestSet(final @NotNull Path path) {
        final String pathStr = path.toString();
        testSetsDirByPath.remove(pathStr);

        final List<UUID> ids = testSetCaseIds.remove(pathStr);
        if (ids != null) {
            for (final UUID id : ids)
                testCasesById.remove(id);
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
        Logger.info("Removed test set package at: " + pathStr);
    }

    void removeTestRunPackage(final @NotNull Path path) {
        final String pathStr = path.toString();
        testRunPackagesByPath.remove(pathStr);
        Logger.info("Removed test run package at: " + pathStr);
    }

    void addTestProject(final @NotNull TestProjectDirectoryDto tp) {
        testProjectsByPath.put(tp.getPath().toString(), tp);
        testCasesMainDirsByPath.put(tp.getTestCasesDirectory().getPath().toString(), tp.getTestCasesDirectory());
        testRunsMainDirsByPath.put(tp.getTestRunsDirectory().getPath().toString(), tp.getTestRunsDirectory());
    }

    void addTestProjectMarker(final @NotNull Project project, final @NotNull TestProjectDirectoryDto tp) {
        final Path markerPath = tp.getPath().resolve(DirectoryType.TP.getMarker());
        Services.getInstance(project, FilesUtil.class).write(project, markerPath, tp.getMarker());
    }

    void updateRunMarker(final @NotNull Project project, final @NotNull Path runPath, final @NotNull TestRunMarker marker) {
        final TestRunDirectoryDto trd = testRunsDirByPath.get(runPath.toString());
        trd.setMarker(marker);
        Services.getInstance(project, FilesUtil.class).write(project, runPath.resolve(DirectoryType.TR.getMarker()), marker);
    }

    void renameNode(final @NotNull Path oldPath, final @NotNull Path newPath) {
        final String oldStr = oldPath.toString();
        final String newStr = newPath.toString();

        renameMapEntry(testProjectsByPath, oldStr, newStr, dto -> dto.setPath(newPath));
        renameMapEntry(testSetsDirByPath, oldStr, newStr, dto -> dto.setPath(newPath));
        renameMapEntry(testRunsDirByPath, oldStr, newStr, dto -> dto.setPath(newPath));
        renameMapEntry(testSetPackagesByPath, oldStr, newStr, dto -> dto.setPath(newPath));
        renameMapEntry(testRunPackagesByPath, oldStr, newStr, dto -> dto.setPath(newPath));
        renameMapEntry(testCasesMainDirsByPath, oldStr, newStr, dto -> dto.setPath(newPath));
        renameMapEntry(testRunsMainDirsByPath, oldStr, newStr, dto -> dto.setPath(newPath));
        renameMapEntry(testSetCaseIds, oldStr, newStr, ids -> {
        });
        renameMapEntry(testRunsByPath, oldStr, newStr, tr -> {
        });

        updatePath2(testProjectsByPath.get(newStr), newPath);
        updatePath2(testSetsDirByPath.get(newStr), newPath);
        updatePath2(testRunsDirByPath.get(newStr), newPath);
        updatePath2(testSetPackagesByPath.get(newStr), newPath);
        updatePath2(testRunPackagesByPath.get(newStr), newPath);
    }

    List<DirectoryDto> getChildren(final Path parentPath) {
        final String parentStr = parentPath.toString();
        final List<DirectoryDto> children = new ArrayList<>();

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

    private void updatePath2(final @NotNull DirectoryDto dto, final Path newPath) {
        final String newName = newPath.getFileName().toString();
        final Tools tools = Services.getInstance(project, Tools.class);
        dto.setPath2(tools.buildPath2(
                dto.getParent() != null ? dto.getParent().getPath2() : null, newName));
    }

    void clearAll() {
        testCasesById.clear();
        testRunsById.clear();
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
