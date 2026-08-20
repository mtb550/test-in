package org.testin.indexer;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.TestCaseSorter;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

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

        // Every case in the set, in rank order. It used to be a walk from
        // whichever case claimed to be the head, with the ones the walk never
        // reached appended afterward - so a set whose head was lost came back in
        // an order nobody chose, and a case pointed at by nothing looked like it
        // belonged at the end.
        final Set<UUID> seen = new HashSet<>(ids.size());
        final List<TestCaseDto> cases = new ArrayList<>(ids.size());

        for (final UUID id : ids) {
            final TestCaseDto testCase = testCasesById.get(id);
            if (testCase != null && seen.add(id)) cases.add(testCase);
        }

        return TestCaseSorter.sorted(cases);
    }

    void put(final @NotNull Path testSetPath, final @NotNull TestCaseDto testCase) {
        // Every save of a test case arrives here - the update dialog, a grid cell,
        // the details panel, a paste - so the audit is stamped once, here, instead
        // of at each of them. Reading does not come through: the indexing scanner
        // fills the maps straight from the JSON, so opening a project stamps
        // nothing.
        //
        // Known to the index means the case already exists, whatever its fields
        // say - the one question that separates a creation from an update without
        // trusting a value a tester could have typed.
        final String tester = Services.getInstance(project, AppSettingsState.class).testerName;
        if (testCasesById.containsKey(testCase.getId())) testCase.touch(tester);
        else testCase.stampCreated(tester);

        store(testSetPath, testCase);
    }

    /**
     * The one save that stamps nothing: an import writes the audit the file
     * brought with it.
     * <p>
     * The four audit attributes are mappable in the import wizard, so a
     * spreadsheet carrying a case's real author and date says who made it.
     * <p>
     * The ordinary path would have called this a creation and written the
     * importer's own name over all four. That is what the preview showing one
     * thing and the saved file holding another came down to (#66).
     * <p>
     * Nothing is filled in when the columns are absent either: an import with no
     * audit columns produces cases with an empty creator, and empty means "the
     * file did not say" rather than a name nobody chose.
     */
    void putImported(final @NotNull Path testSetPath, final @NotNull TestCaseDto testCase) {
        store(testSetPath, testCase);
    }

    private void store(final @NotNull Path testSetPath, final @NotNull TestCaseDto testCase) {
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

    /**
     * The set's membership and order after a rearrangement.
     *
     * @param moved the cases whose rank actually changed. Only these are
     *              written: the order is a value each case carries now, so a
     *              case that did not move has nothing new to say, and rewriting
     *              it would put a file nobody edited in the tester's next commit
     */
    void updateSequence(final @NotNull Path testSetPath, final @NotNull List<TestCaseDto> sortedList,
                        final @NotNull List<TestCaseDto> moved) {
        final String path = testSetPath.toString();
        final List<UUID> ids = new ArrayList<>(sortedList.size());
        final Set<UUID> newIds = new HashSet<>();

        final Set<UUID> movedIds = new HashSet<>();
        for (final TestCaseDto testCase : moved) movedIds.add(testCase.getId());

        for (final TestCaseDto testCase : sortedList) {
            ids.add(testCase.getId());
            newIds.add(testCase.getId());
            testCasesById.put(testCase.getId(), testCase);

            if (!movedIds.contains(testCase.getId())) continue;

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
