package org.testin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public enum TestCaseStatus {
    REVIEWED(
            "Reviewed"
    ),

    PENDING(
            "Pending"
    ),

    DISABLED(
            "Disabled"
    ),

    TO_BE_UPDATED(
            "To Be Updated"
    );

    private final @NotNull String label;
}
