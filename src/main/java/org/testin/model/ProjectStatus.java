package org.testin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * What a test project is, as far as the tree is concerned.
 * <p>
 * There is no removed state, and deliberately: removing a test project deletes
 * its directory and the automation package with it, so there is nothing left to
 * carry a status. A REMOVED constant was declared here and never assigned by
 * anything - kept for a while in case a marker already on disk held it, and no
 * marker ever did. Its siblings {@link TestSetStatus} and {@link PackageStatus}
 * have never had one.
 */
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

    ARCHIVED(
            "Archived",
            "Archive",
            "Archive test project"
    );

    private final @NotNull String label;
    private final @NotNull String buttonName;
    private final @NotNull String buttonDescription;
}
