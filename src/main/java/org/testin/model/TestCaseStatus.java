package org.testin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

@Getter
@AllArgsConstructor
public enum TestCaseStatus {
    REVIEWED(
            Bundle.message("status.case.reviewed")
    ),

    PENDING(
            Bundle.message("status.case.pending")
    ),

    DISABLED(
            Bundle.message("status.case.disabled")
    ),

    TO_BE_UPDATED(
            Bundle.message("status.case.to.be.updated")
    );

    private final @NotNull String label;
}
