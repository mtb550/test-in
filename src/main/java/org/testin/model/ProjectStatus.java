package org.testin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

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
public enum ProjectStatus implements NodeStatus {
    ACTIVE(
            Bundle.message("status.project.active"),
            Bundle.message("status.project.active.action"),
            Bundle.message("status.project.active.description"),
            true
    ),

    INACTIVE(
            Bundle.message("status.project.inactive"),
            Bundle.message("status.project.inactive.action"),
            Bundle.message("status.project.inactive.description"),
            false
    ),

    ARCHIVED(
            Bundle.message("status.project.archived"),
            Bundle.message("status.project.archived.action"),
            Bundle.message("status.project.archived.description"),
            false
    );

    private final @NotNull String label;
    private final @NotNull String buttonName;
    private final @NotNull String buttonDescription;

    /**
     * See {@link NodeStatus#isActive()} - the tree says nothing beside a node
     * that is in current work.
     */
    private final boolean active;
}
