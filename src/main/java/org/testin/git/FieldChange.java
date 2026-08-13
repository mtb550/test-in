package org.testin.git;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A single changed field inside a {@link TestCaseDiff}. The values are nullable
 * because the compared test-case fields are themselves optional — a reference or
 * module that was never filled in compares as null on one side of the change.
 */
public record FieldChange(@NotNull String fieldName, @Nullable String oldValue, @Nullable String newValue,
                          @NotNull ChangeType changeType) {
}
