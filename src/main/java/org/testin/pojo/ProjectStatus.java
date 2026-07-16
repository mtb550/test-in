package org.testin.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;

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

    private final String description;
    private final String buttonName;
    private final String buttonDescription;
}