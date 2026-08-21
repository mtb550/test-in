package org.testin.git;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum ChangeType {
    CREATE_TEST_CASE(
            "Create Test Case",
            RevertAction.NONE
    ),

    REMOVE_TEST_CASE(
            "Remove Test Case",
            RevertAction.NONE
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
    ),

    // Everything below is about a file that is not a test case. None of them
    // reverts: a run's results and a node's marker are written by the plugin as
    // work happens, and putting one back is undoing the work rather than undoing
    // an edit. They are listed so they can be seen and committed (#66).

    CREATE_TEST_RUN(
            "Create Test Run",
            RevertAction.NONE
    ),

    CHANGE_TEST_RUN(
            "Change Test Run",
            RevertAction.NONE
    ),

    REMOVE_TEST_RUN(
            "Remove Test Run",
            RevertAction.NONE
    ),

    CREATE_MARKER(
            "Create Marker",
            RevertAction.NONE
    ),

    CHANGE_MARKER(
            "Change Marker",
            RevertAction.NONE
    ),

    REMOVE_MARKER(
            "Remove Marker",
            RevertAction.NONE
    ),

    CREATE_FILE(
            "Create File",
            RevertAction.NONE
    ),

    CHANGE_FILE(
            "Change File",
            RevertAction.NONE
    ),

    REMOVE_FILE(
            "Remove File",
            RevertAction.NONE
    );

    private final @NotNull String label;

    /**
     * How a row of this kind is put back. Add and remove revert nothing:
     * creating or deleting a whole test case has no field to put back.
     */
    private final @NotNull RevertAction revertAction;

    /**
     * Whether a row of this kind can be put back at all - which is what greys
     * the revert out. The one reader of what the action is, so no caller has to
     * know that "reverts nothing" and "cannot be reverted" are the same fact.
     */
    public boolean isRevertable() {
        return revertAction != RevertAction.NONE;
    }

    /**
     * The kind of change this label names, empty when nothing names it.
     */
    public static @NotNull Optional<ChangeType> fromLabel(final @NotNull String label) {
        return Arrays.stream(values()).filter(type -> type.label.equals(label)).findFirst();
    }
}
