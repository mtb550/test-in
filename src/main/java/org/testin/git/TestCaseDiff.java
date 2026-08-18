package org.testin.git;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.dto.TestCaseDto;

import java.nio.file.Path;
import java.util.List;

/**
 * One reviewable test-case change. Exactly one side is absent for a pure
 * add or delete: {@code oldState} is null for {@link DiffType#ADDED},
 * {@code newState} is null for {@link DiffType#DELETED}.
 */
public record TestCaseDiff(@NotNull String testCaseId, @NotNull Path relativeFilePath, @NotNull DiffType type,
                           @Nullable TestCaseDto oldState, @Nullable TestCaseDto newState,
                           @NotNull List<FieldChange> fieldChanges) {

    /**
     * The test case this change is about: the side of it that exists.
     * <p>
     * A deletion is about the case that was there; everything else is about the
     * case that is there now. {@code TestCaseDiffFactory} always populates that
     * side - an addition reads the new revision, a deletion the old one, a
     * modification both - so the question has an answer for every diff, and
     * asking it here rather than at each call site is what keeps the two
     * nullable fields from spreading a null check across everything that renders
     * a row.
     *
     * @throws IllegalStateException if a diff was built without the side its own
     *                               type says it must have
     */
    public @NotNull TestCaseDto subject() {
        final TestCaseDto state = type == DiffType.DELETED ? oldState : newState;
        if (state == null) {
            throw new IllegalStateException("A " + type + " change carries no test case: " + relativeFilePath);
        }
        return state;
    }
}
