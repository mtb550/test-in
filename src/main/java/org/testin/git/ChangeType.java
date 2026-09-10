package org.testin.git;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;


@Getter
@AllArgsConstructor
public enum ChangeType {
    CREATE_TEST_CASE(
            Bundle.message("change.create.test.case"),
            RevertAction.NONE
    ),

    REMOVE_TEST_CASE(
            Bundle.message("change.remove.test.case"),
            RevertAction.NONE
    ),

    CHANGE_DESCRIPTION(
            Bundle.message("change.change.description"),
            (current, old) -> current.setDescription(old.getDescription())
    ),

    CHANGE_EXPECTED_RESULT(
            Bundle.message("change.change.expected.result"),
            (current, old) -> current.setExpectedResult(old.getExpectedResult())),

    CHANGE_STEPS(
            Bundle.message("change.change.steps"),
            (current, old) -> current.setSteps(new java.util.ArrayList<>(old.getSteps()))),

    CHANGE_PRIORITY(
            Bundle.message("change.change.priority"),
            (current, old) -> current.setPriority(old.getPriority())
    ),

    CHANGE_GROUP(
            Bundle.message("change.change.group"),
            (current, old) -> current.setGroup(new java.util.ArrayList<>(old.getGroup()))
    ),

    CHANGE_STATUS(
            Bundle.message("change.change.status"),
            (current, old) -> current.setStatus(old.getStatus())
    ),

    CHANGE_REFERENCE(
            Bundle.message("change.change.reference"),
            (current, old) -> current.setReference(old.getReference())
    ),

    CHANGE_MODULE(
            Bundle.message("change.change.module"),
            (current, old) -> current.setModule(old.getModule())
    ),

    CHANGE_TEST_DATA(
            Bundle.message("change.change.test.data"),
            (current, old) -> current.setTestData(old.getTestData())
    ),

    CHANGE_PRECONDITIONS(
            Bundle.message("change.change.preconditions"),
            (current, old) -> current.setPreConditions(old.getPreConditions())
    ),

    // Everything below is about a file that is not a test case. None of them
    // reverts: a run's results and a node's marker are written by the plugin as
    // work happens, and putting one back is undoing the work rather than undoing
    // an edit. They are listed so they can be seen and committed (#66).

    CREATE_TEST_RUN(
            Bundle.message("change.create.test.run"),
            RevertAction.NONE
    ),

    CHANGE_TEST_RUN(
            Bundle.message("change.change.test.run"),
            RevertAction.NONE
    ),

    REMOVE_TEST_RUN(
            Bundle.message("change.remove.test.run"),
            RevertAction.NONE
    ),

    CREATE_MARKER(
            Bundle.message("change.create.marker"),
            RevertAction.NONE
    ),

    CHANGE_MARKER(
            Bundle.message("change.change.marker"),
            RevertAction.NONE
    ),

    REMOVE_MARKER(
            Bundle.message("change.remove.marker"),
            RevertAction.NONE
    ),

    CREATE_FILE(
            Bundle.message("change.create.file"),
            RevertAction.NONE
    ),

    CHANGE_FILE(
            Bundle.message("change.change.file"),
            RevertAction.NONE
    ),

    REMOVE_FILE(
            Bundle.message("change.remove.file"),
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

}
