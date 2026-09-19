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

package org.testin.git;

import org.testin.model.Priority;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Mapper;
import org.testin.util.RealMapper;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

/**
 * Everything the pending-commits review shows comes from this class, and it had
 * no tests. It is deliberately IDE-free - it takes the before and after file
 * contents as plain strings - so the whole review model can be built and
 * asserted here, without a repository or a change list.
 */
public class PendingChangeFactoryTest {

    private static final Path PATH = Path.of("Test Cases", "login", "case.tc");

    /**
     * {@link Mapper} is a project service with a private constructor, so the
     * platform normally builds it. It holds nothing but a configured Jackson
     * mapper, so one built here behaves identically - and reaching for it this
     * way keeps the production class unchanged for the sake of a test.
     */
    private static TestCaseDto testCase(final String description) {
        return TestCaseDto.builder()
                .description(description)
                .expectedResult("the balance is shown")
                .steps(new ArrayList<>(List.of("open the app", "log in")))
                .priority(Priority.LOW)
                .module("payments")
                .build();
    }

    private static String json(final TestCaseDto dto) {
        return RealMapper.build().writeValueAsString(dto);
    }

    @Test
    public void anAddedFileIsOneCreateChangeCarryingTheDescription() {
        final TestCaseDto added = testCase("a brand new case");

        final PendingChange diff = PendingChangeFactory.fromFile(
                DiffType.ADDED, "", json(added), PATH, RealMapper.build(), id -> Optional.empty());

        assertNotNull(diff);
        assertEquals(diff.type(), DiffType.ADDED);
        assertEquals(diff.testCaseId(), added.getId().toString());
        assertEquals(diff.relativeFilePath(), PATH);
        assertTrue(diff.committed().getDescription().isEmpty(), "an added file has no before state");

        assertEquals(diff.fieldChanges().size(), 1);
        final FieldChange change = diff.fieldChanges().getFirst();
        assertEquals(change.changeType(), ChangeType.CREATE_TEST_CASE);
        assertEquals(change.oldValue(), "");
        assertEquals(change.newValue(), "a brand new case");
    }

    @Test
    public void aDeletedFileIsOneRemoveChangeCarryingTheDescription() {
        final TestCaseDto removed = testCase("a case that is going away");

        final PendingChange diff = PendingChangeFactory.fromFile(
                DiffType.DELETED, json(removed), "", PATH, RealMapper.build(), id -> Optional.empty());

        assertNotNull(diff);
        assertEquals(diff.type(), DiffType.DELETED);
        assertEquals(diff.committed().getDescription(), "a case that is going away");

        assertEquals(diff.fieldChanges().size(), 1);
        final FieldChange change = diff.fieldChanges().getFirst();
        assertEquals(change.changeType(), ChangeType.REMOVE_TEST_CASE);
        assertEquals(change.oldValue(), "a case that is going away");
        assertEquals(change.newValue(), "");
    }

    @Test
    public void aModifiedFileListsOnlyTheFieldsThatChanged() {
        final TestCaseDto before = testCase("the original description");
        final TestCaseDto after = testCase("the original description")
                .setId(before.getId())
                .setModule("billing")
                .setPriority(Priority.HIGH);

        final PendingChange diff = PendingChangeFactory.fromFile(
                DiffType.MODIFIED, json(before), json(after), PATH, RealMapper.build(), id -> Optional.empty());

        assertNotNull(diff);
        assertEquals(diff.type(), DiffType.MODIFIED);
        assertEquals(diff.committed().getDescription(), "the original description");

        assertEquals(diff.fieldChanges().size(), 2, "two fields moved, so two rows in the review");
        assertEquals(diff.fieldChanges().stream().map(FieldChange::changeType).collect(Collectors.toSet()),
                Set.of(ChangeType.CHANGE_PRIORITY, ChangeType.CHANGE_MODULE));
    }

    /**
     * Git reports a file as modified for reasons no compared field shows - a
     * reorder, an audit stamp, a reformat. It is still a change, and the commit
     * stages only what the review lists, so dropping it would leave a modified
     * file that nothing in the plugin could ever commit. It gets one row saying
     * as much (#66).
     */
    @Test
    public void aModifiedFileWithNoComparedFieldStillGetsARow() {
        final TestCaseDto unchanged = testCase("identical on both sides");

        final PendingChange change = PendingChangeFactory.fromFile(
                DiffType.MODIFIED, json(unchanged), json(unchanged), PATH, RealMapper.build(), id -> Optional.empty());

        assertNotNull(change, "a file git calls modified is a change to commit");
        assertEquals(change.fieldChanges().size(), 1);
        assertEquals(change.fieldChanges().getFirst().changeType(), ChangeType.CHANGE_FILE);
    }

    /**
     * A verdict is its own change, named by the case it is about: since #305 a
     * run's results are one file each, so what a tester reviews is the result
     * that changed rather than a line saying a run changed somehow.
     */
    @Test
    public void aResultIsItsOwnKindOfChange() {
        final UUID caseId = UUID.randomUUID();
        final String before = """
                {"id":"%s","status":"PENDING"}""".formatted(caseId);
        final String after = """
                {"id":"%s","status":"PASSED","actualResult":"Signed in"}""".formatted(caseId);

        final PendingChange change = PendingChangeFactory.fromFile(
                DiffType.MODIFIED, before, after, Path.of("Test Runs", "cycle 4", caseId + ".ri"), RealMapper.build(), id -> Optional.empty());

        assertEquals(change.subject(), ChangeSubject.RUN_ITEM);
        assertEquals(change.testCaseId(), caseId.toString(), "the result says which case it is about");
        assertFalse(change.isRevertible(), "a verdict is a record of work, not an edit to undo");
        assertFalse(change.fieldChanges().isEmpty(), "and the row says what the verdict became");
        assertEquals(change.fieldChanges().getFirst().newValue(), "Passed");
    }

    /**
     * A run's own facts are in its marker, so a .tr that changed says which of
     * them did - what used to be one "the run changed" line (#305, D6).
     */
    @Test
    public void aRunsMarkerSaysWhichOfItsFactsChanged() {
        final PendingChange change = PendingChangeFactory.fromFile(
                DiffType.MODIFIED,
                "{\"status\":\"CREATED\",\"createdBy\":\"mtb\",\"configuration\":{\"PLATFORM\":\"Web\"}}",
                "{\"status\":\"IN_PROGRESS\",\"createdBy\":\"mtb\",\"configuration\":{\"PLATFORM\":\"Mobile\"}}",
                Path.of("Test Runs", "cycle 4", ".tr"), RealMapper.build(), id -> Optional.empty());

        assertEquals(change.subject(), ChangeSubject.MARKER);
        assertTrue(change.fieldChanges().stream().anyMatch(field -> field.newValue().equals("Mobile")),
                "the configuration the tester changed is a row: " + change.fieldChanges());
    }

    /**
     * A marker carries no test data, and it is still a change: archiving a
     * project is a marker edit and nothing else, so a review that hid markers
     * left the tester unable to commit it.
     */
    @Test
    public void aMarkerChangeIsListedWithItsStatus() {
        final PendingChange change = PendingChangeFactory.fromFile(
                DiffType.MODIFIED,
                "{\"status\":\"ACTIVE\",\"createdBy\":\"mtb\"}",
                "{\"status\":\"ARCHIVED\",\"createdBy\":\"mtb\"}",
                Path.of("Test Cases", "login", ".ts"), RealMapper.build(), id -> Optional.empty());

        assertNotNull(change);
        assertEquals(change.subject(), ChangeSubject.MARKER);
        assertEquals(change.name(), "login", "a marker is named by the node it belongs to");
        assertFalse(change.isRevertible());

        final FieldChange status = change.fieldChanges().getFirst();
        assertEquals(status.changeType(), ChangeType.CHANGE_MARKER);
        assertEquals(status.oldValue(), "ACTIVE");
        assertEquals(status.newValue(), "ARCHIVED");
    }

    /**
     * A revision that is absent when it should be there is a broken change, not
     * an empty one: reading it as a default test case would show the tester a
     * diff against a case that never existed.
     */
    @Test
    public void aMissingRevisionFailsByName() {
        final TestCaseDto present = testCase("only one side survived");

        assertThrows(IllegalStateException.class, () -> PendingChangeFactory.fromFile(
                DiffType.MODIFIED, "", json(present), PATH, RealMapper.build(), id -> Optional.empty()));

        assertThrows(IllegalStateException.class, () -> PendingChangeFactory.fromFile(
                DiffType.MODIFIED, json(present), "", PATH, RealMapper.build(), id -> Optional.empty()));

        assertThrows(IllegalStateException.class, () -> PendingChangeFactory.fromFile(
                DiffType.ADDED, json(present), "", PATH, RealMapper.build(), id -> Optional.empty()));

        assertThrows(IllegalStateException.class, () -> PendingChangeFactory.fromFile(
                DiffType.DELETED, "", json(present), PATH, RealMapper.build(), id -> Optional.empty()));
    }

    /**
     * Every diff can name the test case it is about, whichever side of the
     * change survives it. The review shows that description on every row, so a
     * diff that could not answer would be a row with no case on it.
     */
    @Test
    public void everyKindOfChangeCanNameItsTestCase() {
        final TestCaseDto added = testCase("added");
        final TestCaseDto before = testCase("before");
        final TestCaseDto after = testCase("after").setId(before.getId());

        assertEquals(PendingChangeFactory.fromFile(DiffType.ADDED, "", json(added), PATH, RealMapper.build(), id -> Optional.empty())
                .name(), "added");

        assertEquals(PendingChangeFactory.fromFile(DiffType.DELETED, json(before), "", PATH, RealMapper.build(), id -> Optional.empty())
                .name(), "before", "a deletion is about the case that was there");

        assertEquals(PendingChangeFactory.fromFile(DiffType.MODIFIED, json(before), json(after), PATH, RealMapper.build(), id -> Optional.empty())
                .name(), "after", "a modification is about the case as it is now");
    }

    /**
     * The round trip the review depends on: what a test case is written as on
     * disk has to come back as the same test case, or every diff is noise.
     */
    @Test
    public void aTestCaseSurvivesBeingWrittenAndReadBack() {
        final TestCaseDto original = testCase("survives the round trip");

        // Read back as the side a change keeps - the committed one, which a
        // deletion carries.
        final PendingChange diff = PendingChangeFactory.fromFile(
                DiffType.DELETED, json(original), "", PATH, RealMapper.build(), id -> Optional.empty());

        assertNotNull(diff);
        final TestCaseDto readBack = diff.committed();

        assertEquals(TestCaseChangeComparator.compare(original, readBack), List.of(),
                "a test case written and read back must compare as unchanged");
    }
}
