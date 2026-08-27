package org.testin.git;

import org.testin.model.TestEditorAttributes;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Group;
import org.testin.model.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Compares user-editable test-case fields and describes how each change can be reverted.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class TestCaseChangeComparator {

    static @NotNull List<FieldChange> compare(final @NotNull TestCaseDto oldState, final @NotNull TestCaseDto newState) {
        final @NotNull List<FieldChange> changes = new ArrayList<>();
        addIfChanged(changes, TestEditorAttributes.DESCRIPTION.getName(), oldState.getDescription(), newState.getDescription(), ChangeType.CHANGE_DESCRIPTION);
        addIfChanged(changes, TestEditorAttributes.EXPECTED_RESULT.getName(), oldState.getExpectedResult(), newState.getExpectedResult(), ChangeType.CHANGE_EXPECTED_RESULT);
        addIfChanged(changes, TestEditorAttributes.STEPS.getName(), formatSteps(oldState), formatSteps(newState), ChangeType.CHANGE_STEPS);
        addIfChanged(changes, TestEditorAttributes.PRIORITY.getName(), oldState.getPriority().name(), newState.getPriority().name(), ChangeType.CHANGE_PRIORITY);
        addIfChanged(changes, TestEditorAttributes.STATUS.getName(), oldState.getStatus().name(), newState.getStatus().name(), ChangeType.CHANGE_STATUS);
        addIfChanged(changes, TestEditorAttributes.REFERENCE.getName(), oldState.getReference(), newState.getReference(), ChangeType.CHANGE_REFERENCE);
        addIfChanged(changes, TestEditorAttributes.MODULE.getName(), oldState.getModule(), newState.getModule(), ChangeType.CHANGE_MODULE);
        addIfChanged(changes, TestEditorAttributes.TEST_DATA.getName(), oldState.getTestData(), newState.getTestData(), ChangeType.CHANGE_TEST_DATA);
        addIfChanged(changes, TestEditorAttributes.PRE_CONDITIONS.getName(), oldState.getPreConditions(), newState.getPreConditions(), ChangeType.CHANGE_PRECONDITIONS);

        if (!Objects.equals(oldState.getGroup(), newState.getGroup())) {
            changes.add(new FieldChange(
                    TestEditorAttributes.GROUP.getName(), groupNames(oldState), groupNames(newState), ChangeType.CHANGE_GROUP));
        }
        return changes;
    }

    private static void addIfChanged(final @NotNull List<FieldChange> changes, final @NotNull String field, final @NotNull String oldValue, final @NotNull String newValue, final @NotNull ChangeType type) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(new FieldChange(field, oldValue, newValue, type));
        }
    }

    private static @NotNull String groupNames(final @NotNull TestCaseDto testCase) {
        return testCase.getGroup().stream().map(Group::getName)
                .reduce((first, second) -> first + ", " + second).orElse("");
    }

    private static @NotNull String formatSteps(final @NotNull TestCaseDto testCase) {
        return String.join("\n", testCase.getSteps());
    }
}
