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
}
