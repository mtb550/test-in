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

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.TestCaseOrder;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns test-case lookup and the persisted linked-list sequence for each test set.
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
final class TestCaseSequenceStore {

    private final @NotNull Project p;
    private final @NotNull Map<UUID, TestCaseDto> testCasesById = new ConcurrentHashMap<>();
    private final @NotNull Map<String, List<UUID>> testSetCaseIds = new ConcurrentHashMap<>();


    @NotNull Map<UUID, TestCaseDto> getTestCasesById() {
        return testCasesById;
    }

    @NotNull Map<String, List<UUID>> getTestSetCaseIds() {
        return testSetCaseIds;
    }

    /**
     * The list a test set's case ids are kept in, built in one place so every
     * one of them is the same kind.
     * <p>
     * Copy on write. The scan fills a set's list from a parallel stream, the EDT
     * iterates it to draw the cards, and a save, a removal or a drag rewrites
     * it - so an iteration must never see a half-applied change, and a copy on
     * each of the rare writes is cheaper than a lock on each of the many reads.
     * <p>
     * It has one owner because it did not: the scan built a synchronized list
     * and a drag-reorder put a plain ArrayList back over it, so after the first
     * rearrangement the set's ids were shared with no protection at all - and
     * even the scan's list was iterated without the monitor a synchronized list
     * requires, which is a ConcurrentModificationException out of a paint or a
     * card silently missing (#66, finding 84).
     */
    static @NotNull List<UUID> caseIds(final @NotNull Collection<UUID> initial) {
        return new CopyOnWriteArrayList<>(initial);
    }

    // UC-INTERNAL-004, Rule-INTERNAL-030
    @NotNull List<TestCaseDto> getForTestSet(final @NotNull Path testSetPath) {
        final @NotNull List<UUID> ids = testSetCaseIds.getOrDefault(testSetPath.toString(), List.of());
        if (ids.isEmpty()) return List.of();

        // Every case in the set, in rank order. It used to be a walk from
        // whichever case claimed to be the head, with the ones the walk never
        // reached appended afterward - so a set whose head was lost came back in
        // an order nobody chose, and a case pointed at by nothing looked like it
        // belonged at the end.
        final @NotNull Set<UUID> seen = new HashSet<>(ids.size());
        final @NotNull List<TestCaseDto> cases = new ArrayList<>(ids.size());

        for (final UUID id : ids) {
            // An id the index has no case for is one the scanner could not read;
            // it is left out rather than drawn as a blank row.
            if (seen.add(id)) Optional.ofNullable(testCasesById.get(id)).ifPresent(cases::add);
        }

        return TestCaseOrder.ordered(cases);
    }

    /**
     * UC-INTERNAL-004, Rule-INTERNAL-033, Rule-INTERNAL-034.
     * <p>
     * Saves a test case, and says whether it had anything to save.
     * <p>
     * Every save arrives here - the update dialog, a grid cell, the details
     * panel, a paste - so the audit is stamped once, here, instead of at each of
     * them. Reading does not come through: the indexing scanner fills the maps
     * straight from the JSON, so opening a project stamps nothing.
     * <p>
     * Which is also why a save that changes nothing is refused here. Opening a
     * field to read it and pressing Enter used to record the tester as having
     * edited the case, because the stamp was written on the fact of a save
     * rather than on anything having changed (#164). One funnel, one answer, and
     * the four ways in are fixed together.
     *
     * @return false when the file already holds this case exactly - nothing
     * stamped and nothing written, so the caller has nothing to confirm and
     * nothing to take back.
     */
    boolean put(final @NotNull Path testSetPath, final @NotNull TestCaseDto testCase) {
        // Asked before the stamp, because the stamp is itself a change: touch()
        // writes a new updatedAt, and anything compared after it differs by the
        // one field this exists to avoid writing.
        if (Services.getInstance(p, TestDataFiles.class).alreadyHolds(p, fileOf(testSetPath, testCase.getId()), testCase)) return false;

        // Known to the index means the case already exists, whatever its fields
        // say - the one question that separates a creation from an update without
        // trusting a value a tester could have typed.
        final @NotNull String tester = Services.getInstance(p, AppSettingsState.class).testerName;
        if (testCasesById.containsKey(testCase.getId())) testCase.touch(tester);
        else testCase.stampCreated(tester);

        store(testSetPath, testCase);
        return true;
    }

    /**
     * Where a test case lives, from the set that holds it and its id. One owner,
     * because three things ask - the save, the unchanged check that now precedes
     * it, and the delete.
     */
    private static @NotNull Path fileOf(final @NotNull Path testSetPath, final @NotNull UUID testCaseId) {
        return testSetPath.resolve(testCaseId + ".json");
    }

    /**
     * UC-INTERNAL-004, Rule-INTERNAL-035.
     * <p>
     * The save that stamps nothing, for the two callers whose audit is already
     * decided: an import writes the audit the file brought with it, and an undo
     * writes the audit the case had before the change being taken back.
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
    void putVerbatim(final @NotNull Path testSetPath, final @NotNull TestCaseDto testCase) {
        store(testSetPath, testCase);
    }

    private void store(final @NotNull Path testSetPath, final @NotNull TestCaseDto testCase) {
        final @NotNull String path = testSetPath.toString();
        testCasesById.put(testCase.getId(), testCase);
        final @NotNull List<UUID> ids = testSetCaseIds.computeIfAbsent(path, ignored -> caseIds(List.of()));
        if (!ids.contains(testCase.getId())) ids.add(testCase.getId());

        Services.getInstance(p, TestDataFiles.class)
                .write(p, fileOf(testSetPath, testCase.getId()), testCase);
    }

    void remove(final @NotNull Path testSetPath, final @NotNull UUID testCaseId) {
        testCasesById.remove(testCaseId);
        Optional.ofNullable(testSetCaseIds.get(testSetPath.toString()))
                .ifPresent(ids -> ids.remove(testCaseId));

        // Through the writer like the write paths beside it, so OwnWrites
        // claims the delete and our own removal is not read as an external
        // change worth a rescan (#117). stopAt is the set itself: a set
        // outlives its last case, so nothing above the file is pruned.
        Services.getInstance(p, TestDataFiles.class)
                .delete(p, fileOf(testSetPath, testCaseId), testSetPath);
    }

    /**
     * UC-INTERNAL-004, Rule-INTERNAL-031.
     * <p>
     * The set's membership and order after a rearrangement.
     *
     * @param moved the cases whose rank actually changed. Only these are
     *              written: the order is a value each case carries now, so a
     *              case that stayed put has nothing new to say, and rewriting it
     *              would put an untouched file in the tester's next commit
     */
    void updateSequence(final @NotNull Path testSetPath, final @NotNull List<TestCaseDto> orderedList, final @NotNull List<TestCaseDto> moved) {
        final @NotNull String path = testSetPath.toString();
        final @NotNull List<UUID> ids = new ArrayList<>(orderedList.size());
        final @NotNull Set<UUID> newIds = new HashSet<>();

        final @NotNull Set<UUID> movedIds = new HashSet<>();
        for (final TestCaseDto testCase : moved) movedIds.add(testCase.getId());

        final @NotNull String tester = Services.getInstance(p, AppSettingsState.class).testerName;

        for (final TestCaseDto testCase : orderedList) {
            ids.add(testCase.getId());
            newIds.add(testCase.getId());

            // First sight of a case is its creation here too, not only in put.
            // Both methods register cases in the same map and put decides
            // creation-or-update by asking that map - so whichever of them sees a
            // case first has to be the one that stamps it. This one saw a newly
            // created case first, registered it unstamped, and put then found it
            // already known and recorded an update: the case was born with a
            // modifier and no creator, which is what the details panel showed.
            if (!testCasesById.containsKey(testCase.getId())) testCase.stampCreated(tester);

            testCasesById.put(testCase.getId(), testCase);

            if (!movedIds.contains(testCase.getId())) continue;

            Services.getInstance(p, TestDataFiles.class)
                    .write(p, testSetPath.resolve(testCase.getId() + ".json"), testCase);
        }

        // Whatever the set held and no longer holds stops being indexed at all.
        Optional.ofNullable(testSetCaseIds.get(path)).ifPresent(oldIds -> oldIds.stream()
                .filter(id -> !newIds.contains(id))
                .forEach(testCasesById::remove));
        testSetCaseIds.put(path, caseIds(ids));
    }

    void removeForTestSet(final @NotNull String path) {
        Optional.ofNullable(testSetCaseIds.remove(path))
                .ifPresent(ids -> ids.forEach(testCasesById::remove));
    }

    void clear() {
        testCasesById.clear();
        testSetCaseIds.clear();
    }
}
