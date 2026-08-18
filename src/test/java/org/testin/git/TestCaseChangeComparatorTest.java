package org.testin.git;

import org.testin.model.Group;
import org.testin.model.Priority;
import org.testin.model.TestCaseStatus;
import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestCaseChangeComparatorTest {

    private static TestCaseDto base() {
        return TestCaseDto.builder()
                .description("a login case")
                .expectedResult("the balance is shown")
                .steps(new ArrayList<>(List.of("open the app", "log in")))
                .priority(Priority.LOW)
                .status(TestCaseStatus.PENDING)
                .reference("REF-1")
                .module("payments")
                .testData("account=1")
                .preConditions("authenticated")
                .group(new ArrayList<>(List.of(Group.SMOKE)))
                .build();
    }

    private static FieldChange onlyChange(final TestCaseDto after) {
        final List<FieldChange> changes = TestCaseChangeComparator.compare(base(), after);
        assertEquals(changes.size(), 1, "expected exactly one changed field, got " + changes);
        return changes.getFirst();
    }

    @Test
    public void comparesAllEditableFields() {
        final TestCaseDto oldState = TestCaseDto.builder().build();
        final TestCaseDto newState = TestCaseDto.builder()
                .description("new description")
                .expectedResult("new result")
                .steps(List.of("first", "second"))
                .priority(Priority.HIGH)
                .status(TestCaseStatus.REVIEWED)
                .reference("REF-1")
                .module("payments")
                .testData("account=1")
                .preConditions("authenticated")
                .group(List.of(Group.SMOKE))
                .build();

        final List<FieldChange> changes = TestCaseChangeComparator.compare(oldState, newState);

        assertEquals(changes.size(), 10);
        assertTrue(changes.stream().anyMatch(change -> change.changeType() == ChangeType.CHANGE_STEPS));
        assertTrue(changes.stream().anyMatch(change -> change.changeType() == ChangeType.CHANGE_PRECONDITIONS));
    }

    /**
     * The case that decides whether a file is offered for review at all: with
     * nothing changed the factory answers null and the tester is not shown a row
     * with no content in it.
     */
    @Test
    public void twoIdenticalTestCasesHaveNothingToReview() {
        assertEquals(TestCaseChangeComparator.compare(base(), base()), List.of());
    }

    @Test
    public void oneChangedFieldIsOneRowCarryingBothValues() {
        final FieldChange change = onlyChange(base().setModule("billing"));

        assertEquals(change.changeType(), ChangeType.CHANGE_MODULE);
        assertEquals(change.fieldName(), "Module");
        assertEquals(change.oldValue(), "payments");
        assertEquals(change.newValue(), "billing");
    }

    /**
     * Steps are one field, not one row per step: a tester who rewrote the third
     * of five sees a single Steps change with both versions in it, which is what
     * the review's before/after panes render.
     */
    @Test
    public void stepsCompareAsOneBlockRatherThanLineByLine() {
        final FieldChange change = onlyChange(
                base().setSteps(new ArrayList<>(List.of("open the app", "log in as admin"))));

        assertEquals(change.changeType(), ChangeType.CHANGE_STEPS);
        assertEquals(change.oldValue(), "open the app\nlog in");
        assertEquals(change.newValue(), "open the app\nlog in as admin");
    }

    @Test
    public void addingAGroupIsOneChangeListingAllOfThem() {
        final FieldChange change = onlyChange(
                base().setGroup(new ArrayList<>(List.of(Group.SMOKE, Group.REGRESSION))));

        assertEquals(change.changeType(), ChangeType.CHANGE_GROUP);
        assertEquals(change.oldValue(), "Smoke");
        assertEquals(change.newValue(), "Smoke, Regression");
    }

    /**
     * Groups are compared as an ordered list, so reordering them is a change.
     * That is deliberate rather than incidental: the order is what is written to
     * the file, so a reorder is a real difference a reviewer can see in the diff.
     */
    @Test
    public void reorderingGroupsIsAChangeBecauseTheFileChanged() {
        final TestCaseDto before = base().setGroup(new ArrayList<>(List.of(Group.SMOKE, Group.REGRESSION)));
        final TestCaseDto after = base().setGroup(new ArrayList<>(List.of(Group.REGRESSION, Group.SMOKE)));

        final List<FieldChange> changes = TestCaseChangeComparator.compare(before, after);

        assertEquals(changes.size(), 1);
        assertEquals(changes.getFirst().changeType(), ChangeType.CHANGE_GROUP);
    }

    /**
     * Changing a field back to what it was leaves nothing to review, so a tester
     * who undoes an edit by hand does not have to revert it as well.
     */
    @Test
    public void editingAFieldBackToItsOldValueLeavesNothing() {
        final TestCaseDto edited = base().setDescription("something else").setDescription("a login case");

        assertEquals(TestCaseChangeComparator.compare(base(), edited), List.of());
    }
}
