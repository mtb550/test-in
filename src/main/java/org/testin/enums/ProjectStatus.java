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
