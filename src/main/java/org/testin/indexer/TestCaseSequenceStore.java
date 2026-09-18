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
import java.util.stream.Collectors;

/**
 * Owns test-case lookup and the persisted linked-list sequence for each test set.
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
final class TestCaseSequenceStore {

    private final @NotNull Project p;
    private final @NotNull Map<UUID, TestCaseDto> testCasesById = new ConcurrentHashMap<>();
    private final @NotNull Map<String, List<UUID>> testSetCaseIds = new ConcurrentHashMap<>();

    /**
     * UC-INTERNAL-004, Rule-INTERNAL-084.
     * <p>
     * The test cases read from a file the plugin did not name, and that file.
     * <p>
     * A case read from {@code login.json} was saved to {@code <id>.json} and the
     * hand-named file was left holding the case as it was, so the next scan read
     * two files claiming one identity and kept whichever it read last - the edit
     * there or gone at random (#66, finding 175). Known here, the save takes the
     * hand-named file away once the case is filed under its id, and removing the
     * case removes the file it is really in.
     */
    private final @NotNull Map<UUID, Path> handNamed = new ConcurrentHashMap<>();

    /**
     * UC-SHARE-002, Rule-SHARE-001.
     * <p>
     * The case files the last scan of each test set could not read, by set.
     */
    private final @NotNull Map<String, Set<String>> unreadable = new ConcurrentHashMap<>();

    /**
     * UC-SHARE-002, Rule-SHARE-001.
     * <p>
     * The test case files in this set that the last scan could not read, by
     * name. An export names them rather than counting them: a tester who
     * recognizes the file knows whether the export is worth sending (#263).
     * Asked here because the scan is what read them; the export used to walk
     * the folder through the VFS to work it out again (#66, finding 278).
     */
    @NotNull Set<String> unreadableIn(final @NotNull Path testSetPath) {
        return Set.copyOf(unreadable.getOrDefault(testSetPath.toString(), Set.of()));
    }


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
     * Every edit arrives here - the update dialog, a grid cell, the details
     * panel - so the audit is stamped once, here, instead of at each of them. A
     * paste does not: a pasted copy is stamped by {@link #updateSequence}, the
     * first to see it, and a pasted cut is saved as it is through
     * {@link #putVerbatim}. Reading does not come through either: the indexing
     * scanner fills the maps straight from the JSON, so opening a project stamps
     * nothing.
     * <p>
     * Which is also why a save that changes nothing is refused here. Opening a
     * field to read it and pressing Enter used to record the tester as having
     * edited the case, because the stamp was written on the fact of a save
     * rather than on anything having changed (#164). One funnel, one answer, and
     * every way in is fixed together.
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

        return store(testSetPath, testCase);
    }

    /**
     * UC-INTERNAL-004, Rule-INTERNAL-084.
     * <p>
     * The file a test case is in now: the one a tester named by hand, when the
     * case was read from one in this set, and its id's file otherwise. Asked by
     * the unchanged check before a save, the delete, and
     * {@link ProjectIndexer#testCaseFile} for a bug report's link (#28).
     */
    @NotNull Path fileOf(final @NotNull Path testSetPath, final @NotNull UUID testCaseId) {
        return Optional.ofNullable(handNamed.get(testCaseId))
                .filter(file -> testSetPath.equals(file.getParent()))
                .orElseGet(() -> named(testSetPath, testCaseId));
    }

    /**
     * The file the plugin writes a test case to, from the set that holds it and
     * its id. One owner, because the scan asks it too, to tell a file the plugin
     * wrote from one written by hand.
     */
    static @NotNull Path named(final @NotNull Path testSetPath, final @NotNull UUID testCaseId) {
        return testSetPath.resolve(testCaseId + ".json");
    }

    /**
     * UC-INTERNAL-004, Rule-INTERNAL-035.
     * <p>
     * The save that stamps nothing, for the callers whose audit is already
     * decided: an import writes the audit the file brought with it, an undo
     * writes the audit the case had before the change being taken back, a
     * pasted cut keeps the audit of the case it still is, and a removed case put
     * back from the review is the case as it was committed.
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
    boolean putVerbatim(final @NotNull Path testSetPath, final @NotNull TestCaseDto testCase) {
        return store(testSetPath, testCase);
    }

    /**
     * UC-INTERNAL-004, Rule-INTERNAL-033.
     * <p>
     * The file, and then the index - architecture rule 2, which every other
     * write in this package keeps and this one had the wrong way round. The
     * index took the case first and the write's answer was dropped, so a write
     * refused by a read-only or locked file still left the grid showing the new
     * value, the set's marker stamped, the undo recorded, the method regenerated
     * and the case counted in "Updated N" - an edit that existed in memory until
     * the next rescan put the old one back (#66, finding 163).
     *
     * @return whether the file holds the case now. The writer has already told
     * the tester when it does not.
     */
    private boolean store(final @NotNull Path testSetPath, final @NotNull TestCaseDto testCase) {
        final @NotNull TestDataFiles files = Services.getInstance(p, TestDataFiles.class);
        final @NotNull Path file = named(testSetPath, testCase.getId());
        if (!files.write(p, file, testCase)) return false;

        // Rule-INTERNAL-084. Filed under its id now, so the hand-named file it
        // was read from goes, after the write and never before it.
        Optional.ofNullable(handNamed.remove(testCase.getId()))
                .filter(original -> !original.equals(file))
                .ifPresent(original -> files.delete(p, original, original.getParent()));

        testCasesById.put(testCase.getId(), testCase);
        final @NotNull List<UUID> ids = testSetCaseIds.computeIfAbsent(testSetPath.toString(), ignored -> caseIds(List.of()));
        if (!ids.contains(testCase.getId())) ids.add(testCase.getId());

        return true;
    }

    void remove(final @NotNull Path testSetPath, final @NotNull UUID testCaseId) {
        testCasesById.remove(testCaseId);
        Optional.ofNullable(testSetCaseIds.get(testSetPath.toString()))
                .ifPresent(ids -> ids.remove(testCaseId));

        // The file it is really in: a hand-named one was left behind, and the
        // case came back with the next scan (Rule-INTERNAL-084).
        final @NotNull Path file = fileOf(testSetPath, testCaseId);
        handNamed.remove(testCaseId, file);

        // Through the writer like the write paths beside it, so OwnWrites
        // claims the delete and our own removal is not read as an external
        // change worth a rescan (#117). stopAt is the set itself: a set
        // outlives its last case, so nothing above the file is pruned.
        Services.getInstance(p, TestDataFiles.class).delete(p, file, testSetPath);
    }

    /**
     * UC-INTERNAL-004, Rule-INTERNAL-031.
     * <p>
     * The set's membership and order after a rearrangement.
     *
     * @param moved the cases whose rank actually changed. These are written, and
     *              so is any case seen here for the first time; nothing else is.
     *              The order is a value each case carries now, so a case that
     *              stayed put has nothing new to say, and rewriting it would put
     *              an untouched file in the tester's next commit
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
            final boolean firstSight = !testCasesById.containsKey(testCase.getId());
            if (firstSight) testCase.stampCreated(tester);

            testCasesById.put(testCase.getId(), testCase);

            // A case seen for the first time is written whatever its rank did. A
            // pasted case keeps the rank it was copied with, and one that already
            // sorts in place is not among the moved - so it lived in memory only,
            // after a cut had deleted its file, until the next rescan lost it
            // (#66, finding 112).
            if (!firstSight && !movedIds.contains(testCase.getId())) continue;

            // Through store, the one write of a case, rather than a path spelled
            // out here beside the method that owns it: written that way, a
            // reorder of a hand-named case wrote <id>.json and left the
            // original, the two files Rule-INTERNAL-084 exists to prevent (#66,
            // finding 229).
            store(testSetPath, testCase);
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
        handNamed.values().removeIf(file -> Path.of(path).equals(file.getParent()));
        unreadable.remove(path);
    }

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-021.
     * <p>
     * The cases a finished scan of one test project read, put in as one move.
     * <p>
     * Added before anything is removed, for the reason
     * {@link IndexerDataStore#swapIn} gives: a case that is on disk in both
     * passes has to be in the index throughout, or a save in the window between
     * them is stamped as a creation because nothing here remembers the case
     * exists (#312, A1).
     * <p>
     * What goes is what this project held and the scan did not find: a test set
     * that was deleted, and a case that was deleted out of a set that remains.
     * The ids are read before the new lists go in, because after that the old
     * ones are no longer there to ask.
     */
    void swapIn(final @NotNull Path projectPath, final @NotNull Map<UUID, TestCaseDto> cases, final @NotNull Map<String, List<UUID>> setCaseIds, final @NotNull Map<UUID, Path> handNamedFiles, final @NotNull Map<String, Set<String>> unreadableCases) {
        final @NotNull Set<UUID> held = testSetCaseIds.entrySet().stream()
                .filter(entry -> Path.of(entry.getKey()).startsWith(projectPath))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toCollection(HashSet::new));

        testCasesById.putAll(cases);
        testSetCaseIds.putAll(setCaseIds);

        testSetCaseIds.keySet().removeIf(path -> Path.of(path).startsWith(projectPath) && !setCaseIds.containsKey(path));

        held.removeAll(cases.keySet());
        held.forEach(testCasesById::remove);

        handNamed.putAll(handNamedFiles);
        handNamed.entrySet().removeIf(entry -> entry.getValue().startsWith(projectPath) && !handNamedFiles.containsKey(entry.getKey()));

        unreadable.putAll(unreadableCases);
        unreadable.keySet().removeIf(set -> Path.of(set).startsWith(projectPath) && !unreadableCases.containsKey(set));
    }

    void clear() {
        testCasesById.clear();
        testSetCaseIds.clear();
        handNamed.clear();
        unreadable.clear();
    }
}
