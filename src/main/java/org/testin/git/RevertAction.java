package org.testin.git;

import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

@FunctionalInterface
public interface RevertAction {

    /**
     * The revert of a change there is no reverting: creating or deleting a whole
     * test case has no field to put back, and a run's results are work rather
     * than an edit.
     * <p>
     * A value rather than a null on {@link ChangeType}, so "cannot be reverted"
     * is stated by the type. What decides whether the row offers a revert at all
     * is {@link ChangeType#isRevertable()}, which is the question the dialog
     * actually asks.
     */
    RevertAction NONE = (currentDto, oldDto) -> {
    };

    void apply(final @NotNull TestCaseDto currentDto, final @NotNull TestCaseDto oldDto);
}
