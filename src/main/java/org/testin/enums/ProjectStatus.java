package org.testin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public enum ProjectStatus {
    ACTIVE(
            "Active",
            "Activate",
            "Activate test project"
    ),

    INACTIVE(
            "Inactive",
            "Deactivate",
            "Deactivate test project"
    ),

    // Reported as never used, and kept: the status is persisted in the test
    // project marker, so a marker already on disk can hold "Removed" and
    // deleting the constant makes it fail to deserialize. No inspection can see
    // that, because the only reader is Jackson (#61).
    REMOVED(
            "Removed",
            "Remove",
            "Remove test project"
    ),

    ARCHIVED(
            "Archived",
            "Archive",
            "Archive test project"
    );

    private final @NotNull String description;
    private final @NotNull String buttonName;
    private final @NotNull String buttonDescription;
}
