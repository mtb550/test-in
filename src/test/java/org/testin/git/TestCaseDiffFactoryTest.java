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
import java.util.stream.Collectors;

import static org.testng.Assert.*;

/**
 * Everything the pending-commits review shows comes from this class, and it had
 * no tests. It is deliberately IDE-free - it takes the before and after file
 * contents as plain strings - so the whole review model can be built and
 * asserted here, without a repository or a change list.
 */
public class TestCaseDiffFactoryTest {

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

        final TestCaseDiff diff = TestCaseDiffFactory.fromJson(
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

        final TestCaseDiff diff = TestCaseDiffFactory.fromJson(
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

        final TestCaseDiff diff = TestCaseDiffFactory.fromJson(
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
     * Git reports a file as modified for reasons the tester has no interest in -
     * a reformat, a field they cannot edit. The review offers nothing rather
     * than a row with no content, which is why the factory can answer null.
     */
    @Test
    public void aModifiedFileWithNothingReviewableIsNotOfferedAtAll() {
        final TestCaseDto unchanged = testCase("identical on both sides");

        assertNull(TestCaseDiffFactory.fromJson(
                DiffType.MODIFIED, json(unchanged), json(unchanged), PATH, mapper()));
    }

    /**
     * A revision that is absent when it should be there is a broken change, not
     * an empty one: reading it as a default test case would show the tester a
     * diff against a case that never existed.
     */
    @Test
    public void aMissingRevisionFailsByName() {
        final TestCaseDto present = testCase("only one side survived");

        assertThrows(IllegalStateException.class, () -> TestCaseDiffFactory.fromJson(
                DiffType.MODIFIED, null, json(present), PATH, mapper()));

        assertThrows(IllegalStateException.class, () -> TestCaseDiffFactory.fromJson(
                DiffType.MODIFIED, json(present), null, PATH, mapper()));

        assertThrows(IllegalStateException.class, () -> TestCaseDiffFactory.fromJson(
                DiffType.ADDED, json(present), null, PATH, mapper()));

        assertThrows(IllegalStateException.class, () -> TestCaseDiffFactory.fromJson(
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

        assertEquals(TestCaseDiffFactory.fromJson(DiffType.ADDED, null, json(added), PATH, mapper())
                .subject().getDescription(), "added");

        assertEquals(TestCaseDiffFactory.fromJson(DiffType.DELETED, json(before), null, PATH, mapper())
                .subject().getDescription(), "before", "a deletion is about the case that was there");

        assertEquals(TestCaseDiffFactory.fromJson(DiffType.MODIFIED, json(before), json(after), PATH, mapper())
                .subject().getDescription(), "after", "a modification is about the case as it is now");
    }

    /**
     * The round trip the review depends on: what a test case is written as on
     * disk has to come back as the same test case, or every diff is noise.
     */
    @Test
    public void aTestCaseSurvivesBeingWrittenAndReadBack() {
        final TestCaseDto original = testCase("survives the round trip");

        final TestCaseDiff diff = TestCaseDiffFactory.fromJson(
                DiffType.ADDED, null, json(original), PATH, mapper());

        assertNotNull(diff);
        final TestCaseDto readBack = diff.newState();
        assertNotNull(readBack);

        assertEquals(TestCaseChangeComparator.compare(original, readBack), List.of(),
                "a test case written and read back must compare as unchanged");
    }
}
