package org.testin.git;

import org.jetbrains.annotations.NotNull;
import org.testin.enums.Group;
import org.testin.mappers.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Compares user-editable test-case fields and describes how each change can be reverted.
 */
final class TestCaseChangeComparator {

    private TestCaseChangeComparator() {
    }

    static @NotNull List<TestCaseDiff.FieldChange> compare(
            final @NotNull TestCaseDto oldState,
            final @NotNull TestCaseDto newState) {
        final List<TestCaseDiff.FieldChange> changes = new ArrayList<>();
        addIfChanged(changes, "Description", oldState.getDescription(), newState.getDescription(), ChangeType.CHANGE_DESCRIPTION);
        addIfChanged(changes, "Expected Result", oldState.getExpectedResult(), newState.getExpectedResult(), ChangeType.CHANGE_EXPECTED_RESULT);
        addIfChanged(changes, "Steps", formatSteps(oldState), formatSteps(newState), ChangeType.CHANGE_STEPS);
        addIfChanged(changes, "Priority", oldState.getPriority().name(), newState.getPriority().name(), ChangeType.CHANGE_PRIORITY);
        addIfChanged(changes, "Status", oldState.getStatus().name(), newState.getStatus().name(), ChangeType.CHANGE_STATUS);
        addIfChanged(changes, "Reference", oldState.getReference(), newState.getReference(), ChangeType.CHANGE_REFERENCE);
        addIfChanged(changes, "Module", oldState.getModule(), newState.getModule(), ChangeType.CHANGE_MODULE);
        addIfChanged(changes, "Test Data", oldState.getTestData(), newState.getTestData(), ChangeType.CHANGE_TEST_DATA);
        addIfChanged(changes, "Preconditions", oldState.getPreConditions(), newState.getPreConditions(), ChangeType.CHANGE_PRECONDITIONS);

        if (!Objects.equals(oldState.getGroup(), newState.getGroup())) {
            changes.add(new TestCaseDiff.FieldChange(
                    "Group", groupNames(oldState), groupNames(newState), ChangeType.CHANGE_GROUP));
        }
        return changes;
    }

    private static void addIfChanged(
            final @NotNull List<TestCaseDiff.FieldChange> changes,
            final @NotNull String field,
            final String oldValue,
            final String newValue,
            final @NotNull ChangeType type) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(new TestCaseDiff.FieldChange(field, oldValue, newValue, type));
        }
    }

    private static String groupNames(final @NotNull TestCaseDto testCase) {
        return testCase.getGroup().stream().map(Group::getName)
                .reduce((first, second) -> first + ", " + second).orElse("");
    }

    private static String formatSteps(final @NotNull TestCaseDto testCase) {
        return String.join("\n", testCase.getSteps());
    }
}
