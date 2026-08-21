package org.testin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Whether a test set is still current. Persisted in the test set marker.
 * <p>
 * Deprecated is not deleted: the cases stay readable and the runs that already
 * used them keep their history. What it changes is what happens next — a
 * deprecated set is retired ({@code DirectoryDto.isRetired()}): drawn gray,
 * ordered after its siblings, and not offered when a new run is configured (#68).
 */
@Getter
@AllArgsConstructor
public enum TestSetStatus {
    ACTIVE(
            "Active",
            "Mark Active",
            "Offer this test set when configuring a run"
    ),

    DEPRECATED(
            "Deprecated",
            "Mark Deprecated",
            "Keep the test set and its history, but stop offering it for new runs"
    );

    private final @NotNull String label;
    private final @NotNull String buttonName;
    private final @NotNull String buttonDescription;
}
