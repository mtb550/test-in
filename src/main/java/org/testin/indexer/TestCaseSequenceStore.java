package org.testin.indexer;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.FilesUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns test-case lookup and the persisted linked-list sequence for each test set.
 */
final class TestCaseSequenceStore {

    private final @NotNull Project project;
    private final @NotNull Map<UUID, TestCaseDto> testCasesById = new ConcurrentHashMap<>();
    private final @NotNull Map<String, List<UUID>> testSetCaseIds = new ConcurrentHashMap<>();

    TestCaseSequenceStore(final @NotNull Project project) {
        this.project = project;
    }

    @NotNull Map<UUID, TestCaseDto> getTestCasesById() {
        return testCasesById;
    }

    @NotNull Map<String, List<UUID>> getTestSetCaseIds() {
        return testSetCaseIds;
    }

    @NotNull List<TestCaseDto> getForTestSet(final @NotNull Path testSetPath) {
        final List<UUID> ids = testSetCaseIds.get(testSetPath.toString());
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        final Map<UUID, TestCaseDto> byId = new HashMap<>(ids.size());
        for (final UUID id : ids) {
            final TestCaseDto testCase = testCasesById.get(id);
            if (testCase != null) byId.put(id, testCase);
        }

        final List<TestCaseDto> result = new ArrayList<>(byId.size());
        final Set<UUID> visited = new HashSet<>();
        TestCaseDto head = byId.values().stream()
                .filter(testCase -> Boolean.TRUE.equals(testCase.getIsHead()))
                .findFirst()
                .orElse(null);

        while (head != null && visited.add(head.getId())) {
            result.add(head);
            head = head.getNext() == null ? null : byId.get(head.getNext());
        }

        for (final UUID id : ids) {
            final TestCaseDto testCase = testCasesById.get(id);
            if (testCase != null && visited.add(testCase.getId())) {
                result.add(testCase);
            }
        }
        return result;
    }

    void put(final @NotNull Path testSetPath, final @NotNull TestCaseDto testCase) {
        final String path = testSetPath.toString();
        testCasesById.put(testCase.getId(), testCase);
        final List<UUID> ids = testSetCaseIds.computeIfAbsent(
                path, ignored -> Collections.synchronizedList(new ArrayList<>()));
        if (!ids.contains(testCase.getId())) ids.add(testCase.getId());

        Services.getInstance(project, FilesUtil.class)
                .write(project, testSetPath.resolve(testCase.getId() + ".json"), testCase);
    }

    void remove(final @NotNull Path testSetPath, final @NotNull UUID testCaseId) {
        testCasesById.remove(testCaseId);
        final List<UUID> ids = testSetCaseIds.get(testSetPath.toString());
        if (ids != null) ids.remove(testCaseId);

        final Path filePath = testSetPath.resolve(testCaseId + ".json");
        try {
            Files.deleteIfExists(filePath);
        } catch (final Exception ex) {
            Logger.error("Failed to delete test case file: " + filePath);
        }
    }

    void updateSequence(final @NotNull Path testSetPath, final @NotNull List<TestCaseDto> sortedList) {
        final String path = testSetPath.toString();
        final List<UUID> ids = new ArrayList<>(sortedList.size());
        final Set<UUID> newIds = new HashSet<>();

        for (int i = 0; i < sortedList.size(); i++) {
            final TestCaseDto testCase = sortedList.get(i);
            testCase.setIsHead(i == 0);
            testCase.setNext(i < sortedList.size() - 1 ? sortedList.get(i + 1).getId() : null);
            ids.add(testCase.getId());
            newIds.add(testCase.getId());
            testCasesById.put(testCase.getId(), testCase);
            Services.getInstance(project, FilesUtil.class)
                    .write(project, testSetPath.resolve(testCase.getId() + ".json"), testCase);
        }

        final List<UUID> oldIds = testSetCaseIds.get(path);
        if (oldIds != null) {
            oldIds.stream()
                    .filter(id -> !newIds.contains(id))
                    .forEach(testCasesById::remove);
        }
        testSetCaseIds.put(path, ids);
    }

    void removeForTestSet(final @NotNull String path) {
        final List<UUID> ids = testSetCaseIds.remove(path);
        if (ids != null) ids.forEach(testCasesById::remove);
    }

    void clear() {
        testCasesById.clear();
        testSetCaseIds.clear();
    }
}
