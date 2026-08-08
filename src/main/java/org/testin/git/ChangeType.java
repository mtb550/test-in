package org.testin.git;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
@AllArgsConstructor
public enum ChangeType {
    CREATE_TEST_CASE(
            "Create Test Case",
            null
    ),

    REMOVE_TEST_CASE(
            "Remove Test Case",
            null
    ),

    CHANGE_DESCRIPTION(
            "Change Description",
            (current, old) -> current.setDescription(old.getDescription())
    ),

    CHANGE_EXPECTED_RESULT(
            "Change Expected Result",
            (current, old) -> current.setExpectedResult(old.getExpectedResult())),

    CHANGE_PRIORITY(
            "Change Priority",
            (current, old) -> current.setPriority(old.getPriority())
    ),

    CHANGE_GROUP(
            "Change Group",
            (current, old) -> current.setGroup(old.getGroup())
    );

    private final String label;
    private final RevertAction revertAction;

    public static @Nullable ChangeType fromLabel(final String label) {
        for (final ChangeType type : values())
            if (type.label.equals(label)) return type;

        return null;
    }
}
