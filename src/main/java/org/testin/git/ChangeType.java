package org.testin.git;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
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

    CHANGE_STEPS(
            "Change Steps",
            (current, old) -> current.setSteps(new java.util.ArrayList<>(old.getSteps()))),

    CHANGE_PRIORITY(
            "Change Priority",
            (current, old) -> current.setPriority(old.getPriority())
    ),

    CHANGE_GROUP(
            "Change Group",
            (current, old) -> current.setGroup(new java.util.ArrayList<>(old.getGroup()))
    ),

    CHANGE_STATUS(
            "Change Status",
            (current, old) -> current.setStatus(old.getStatus())
    ),

    CHANGE_REFERENCE(
            "Change Reference",
            (current, old) -> current.setReference(old.getReference())
    ),

    CHANGE_MODULE(
            "Change Module",
            (current, old) -> current.setModule(old.getModule())
    ),

    CHANGE_TEST_DATA(
            "Change Test Data",
            (current, old) -> current.setTestData(old.getTestData())
    ),

    CHANGE_PRECONDITIONS(
            "Change Preconditions",
            (current, old) -> current.setPreConditions(old.getPreConditions())
    );

    private final @NotNull String label;

    /** Null for add/remove: creating or deleting a whole test case has no field to revert. */
    private final @Nullable RevertAction revertAction;

    public static @Nullable ChangeType fromLabel(final @Nullable String label) {
        for (final ChangeType type : values())
            if (type.label.equals(label)) return type;

        return null;
    }
}
