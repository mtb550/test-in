package org.testin.git;

import org.jetbrains.annotations.NotNull;

/**
 * A single changed field inside a {@link PendingChange}.
 * <p>
 * Both values are always there. A field the tester never filled in is {@code ""}
 * on that side, not null - every compared field on {@link org.testin.model.dto.TestCaseDto}
 * is {@code @NonNull} with an empty default, and an addition or a removal reports
 * the empty string for the side that does not exist. These used to be declared
 * nullable for an absence the model does not have, which pushed a null check onto
 * everything that renders a row.
 */
public record FieldChange(@NotNull String fieldName, @NotNull String oldValue, @NotNull String newValue, @NotNull ChangeType changeType) {
}
