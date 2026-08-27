package org.testin.git;

import org.testin.model.Group;
import org.testin.model.Priority;
import org.testin.model.TestCaseStatus;
import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * Revert is the one destructive button in the pending-commits review, and it is
 * driven entirely by data on this enum (#66).
 * <p>
 * The way it can go wrong without anything failing is a revert that writes the
 * wrong field, which silently corrupts a test case the tester was trying to
 * restore. That is what is asserted here.
 */
public class ChangeTypeRevertTest {

    /**
     * A committed state to revert back to, with every editable field different
     * from {@link #edited()} so a revert writing the wrong field is visible.
     */
    private static TestCaseDto committed() {
        return TestCaseDto.builder()
                .description("committed description")
                .expectedResult("committed result")
                .steps(new ArrayList<>(List.of("committed first", "committed second")))
                .priority(Priority.LOW)
                .status(TestCaseStatus.PENDING)
                .reference("REF-committed")
                .module("committed module")
                .testData("committed data")
                .preConditions("committed preconditions")
                .group(new ArrayList<>(List.of(Group.SMOKE)))
                .build();
    }

    private static TestCaseDto edited() {
        return TestCaseDto.builder()
                .description("edited description")
                .expectedResult("edited result")
                .steps(new ArrayList<>(List.of("edited first")))
                .priority(Priority.HIGH)
                .status(TestCaseStatus.REVIEWED)
                .reference("REF-edited")
                .module("edited module")
                .testData("edited data")
                .preConditions("edited preconditions")
                .group(new ArrayList<>(List.of(Group.REGRESSION)))
                .build();
    }

    @Test
    public void everyChangeTheReviewCanShowCanBeReverted() {
        final List<FieldChange> changes = TestCaseChangeComparator.compare(committed(), edited());

        assertEquals(changes.size(), 10, "every editable field differs between the two states");
        for (final FieldChange change : changes) {
            assertTrue(change.changeType().isRevertable(),
                    change.changeType() + " appears in a review, so it must be revertable");
        }
    }

    @Test
    public void creatingOrRemovingAWholeTestCaseHasNoFieldToRevert() {
        assertFalse(ChangeType.CREATE_TEST_CASE.isRevertable());
        assertFalse(ChangeType.REMOVE_TEST_CASE.isRevertable());
    }

    /**
     * The one that matters: each revert restores its own field and leaves every
     * other one alone. A revert that wrote a neighbouring field would restore
     * the value the tester asked for and quietly discard one they did not.
     */
    @Test
    public void eachRevertRestoresItsOwnFieldAndNothingElse() {
        for (final FieldChange change : TestCaseChangeComparator.compare(committed(), edited())) {
            final TestCaseDto current = edited();
            change.changeType().getRevertAction().apply(current, committed());

            final List<FieldChange> left = TestCaseChangeComparator.compare(committed(), current);

            assertEquals(left.size(), 9,
                    change.changeType() + " should revert exactly one field, leaving nine changed");
            assertTrue(left.stream().noneMatch(remaining -> remaining.changeType() == change.changeType()),
                    change.changeType() + " reverted something other than its own field");
        }
    }

    @Test
    public void revertingEveryChangeRestoresTheCommittedTestCase() {
        final TestCaseDto current = edited();
        for (final FieldChange change : TestCaseChangeComparator.compare(committed(), edited())) {
            change.changeType().getRevertAction().apply(current, committed());
        }

        assertEquals(TestCaseChangeComparator.compare(committed(), current), List.of(),
                "reverting every listed change leaves nothing to review");
    }

    /**
     * A revert must not hand the committed state's own list to the working copy:
     * they would then be the same object, and editing one would change the other.
     */
    @Test
    public void revertingAListCopiesItRatherThanSharingIt() {
        final TestCaseDto committed = committed();
        final TestCaseDto current = edited();

        ChangeType.CHANGE_STEPS.getRevertAction().apply(current, committed);
        ChangeType.CHANGE_GROUP.getRevertAction().apply(current, committed);

        current.getSteps().add("typed after the revert");
        current.getGroup().add(Group.REGRESSION);

        assertEquals(committed.getSteps(), List.of("committed first", "committed second"),
                "the committed steps were shared, not copied");
        assertEquals(committed.getGroup(), List.of(Group.SMOKE),
                "the committed groups were shared, not copied");
    }

    /**
     * Two constants sharing a label no longer misroutes a revert - the dialog
     * carries the change rather than parsing its caption back - but two rows
     * that read the same still leave the tester unable to tell them apart.
     */
    @Test
    public void noTwoChangeTypesShareALabel() {
        final Set<String> labels = new HashSet<>();
        for (final ChangeType type : ChangeType.values()) {
            assertTrue(labels.add(type.getLabel()), type + " repeats another change type's label");
        }
    }

}
