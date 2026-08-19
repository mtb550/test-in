package org.testin.git;

import org.testin.model.Priority;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Mapper;
import org.testng.annotations.Test;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

    private static final Path PATH = Path.of("Test Cases", "login", "case.json");

    /**
     * {@link Mapper} is a project service with a private constructor, so the
     * platform normally builds it. It holds nothing but a configured Jackson
     * mapper, so one built here behaves identically - and reaching for it this
     * way keeps the production class unchanged for the sake of a test.
     */
    private static Mapper mapper() {
        try {
            final Constructor<Mapper> constructor = Mapper.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (final ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not build a Mapper for the test", ex);
        }
    }

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
        return mapper().writeValueAsString(dto);
    }

    @Test
    public void anAddedFileIsOneCreateChangeCarryingTheDescription() {
        final TestCaseDto added = testCase("a brand new case");

        final PendingChange diff = PendingChangeFactory.fromFile(
                DiffType.ADDED, null, json(added), PATH, mapper());

        assertNotNull(diff);
        assertEquals(diff.type(), DiffType.ADDED);
        assertEquals(diff.testCaseId(), added.getId().toString());
        assertEquals(diff.relativeFilePath(), PATH);
        assertNull(diff.oldState(), "an added file has no before state");
        assertNotNull(diff.newState());

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
                DiffType.DELETED, json(removed), null, PATH, mapper());

        assertNotNull(diff);
        assertEquals(diff.type(), DiffType.DELETED);
        assertNotNull(diff.oldState());
        assertNull(diff.newState(), "a deleted file has no after state");

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
                DiffType.MODIFIED, json(before), json(after), PATH, mapper());

        assertNotNull(diff);
        assertEquals(diff.type(), DiffType.MODIFIED);
        assertNotNull(diff.oldState());
        assertNotNull(diff.newState());

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
                DiffType.MODIFIED, json(unchanged), json(unchanged), PATH, mapper());

        assertNotNull(change, "a file git calls modified is a change to commit");
        assertEquals(change.fieldChanges().size(), 1);
        assertEquals(change.fieldChanges().getFirst().changeType(), ChangeType.CHANGE_FILE);
    }

    /**
     * A test run is not a test case, and reading it as one is what put a
     * nameless row in the review - and, for a run that was edited rather than
     * created, no row at all (#66).
     */
    @Test
    public void aTestRunIsItsOwnKindOfChange() {
        final String runJson = """
                {"changeLog":"cycle 4","results":[{"id":"%s","status":"PASSED"},{"id":"%s","status":"FAILED"}]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        final PendingChange added = PendingChangeFactory.fromFile(
                DiffType.ADDED, null, runJson, Path.of("Test Runs", "cycle 4", "cycle 4.json"), mapper());

        assertNotNull(added);
        assertEquals(added.subject(), ChangeSubject.TEST_RUN);
        assertEquals(added.name(), "cycle 4", "the run's own name, not a blank test case description");
        assertEquals(added.testSet(), "", "a run belongs to no test set");
        assertFalse(added.isRevertible(), "a verdict is a record of work, not an edit to undo");
        assertEquals(added.fieldChanges().getFirst().changeType(), ChangeType.CREATE_TEST_RUN);
        assertTrue(added.fieldChanges().getFirst().newValue().contains("2 cases"), "the row says what the run holds");
    }

    /**
     * The case that used to vanish: a run already committed, then executed
     * further. No test-case field differs because none applies, and the review
     * showed nothing - so the results could never be committed from it.
     */
    @Test
    public void anEditedTestRunIsStillOfferedForCommit() {
        final String before = """
                {"results":[{"id":"%s","status":"PENDING"}]}""".formatted(UUID.randomUUID());
        final String after = """
                {"results":[{"id":"%s","status":"PASSED"}]}""".formatted(UUID.randomUUID());

        final PendingChange change = PendingChangeFactory.fromFile(
                DiffType.MODIFIED, before, after, Path.of("Test Runs", "cycle 4", "cycle 4.json"), mapper());

        assertNotNull(change, "an edited run is a change the tester has to be able to commit");
        assertEquals(change.subject(), ChangeSubject.TEST_RUN);
        assertFalse(change.fieldChanges().isEmpty());
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
                Path.of("Test Cases", "login", ".ts"), mapper());

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
                DiffType.MODIFIED, null, json(present), PATH, mapper()));

        assertThrows(IllegalStateException.class, () -> PendingChangeFactory.fromFile(
                DiffType.MODIFIED, json(present), null, PATH, mapper()));

        assertThrows(IllegalStateException.class, () -> PendingChangeFactory.fromFile(
                DiffType.ADDED, json(present), null, PATH, mapper()));

        assertThrows(IllegalStateException.class, () -> PendingChangeFactory.fromFile(
                DiffType.DELETED, null, json(present), PATH, mapper()));
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

        assertEquals(PendingChangeFactory.fromFile(DiffType.ADDED, null, json(added), PATH, mapper())
                .testCase().getDescription(), "added");

        assertEquals(PendingChangeFactory.fromFile(DiffType.DELETED, json(before), null, PATH, mapper())
                .testCase().getDescription(), "before", "a deletion is about the case that was there");

        assertEquals(PendingChangeFactory.fromFile(DiffType.MODIFIED, json(before), json(after), PATH, mapper())
                .testCase().getDescription(), "after", "a modification is about the case as it is now");
    }

    /**
     * The round trip the review depends on: what a test case is written as on
     * disk has to come back as the same test case, or every diff is noise.
     */
    @Test
    public void aTestCaseSurvivesBeingWrittenAndReadBack() {
        final TestCaseDto original = testCase("survives the round trip");

        final PendingChange diff = PendingChangeFactory.fromFile(
                DiffType.ADDED, null, json(original), PATH, mapper());

        assertNotNull(diff);
        final TestCaseDto readBack = diff.newState();
        assertNotNull(readBack);

        assertEquals(TestCaseChangeComparator.compare(original, readBack), List.of(),
                "a test case written and read back must compare as unchanged");
    }
}
