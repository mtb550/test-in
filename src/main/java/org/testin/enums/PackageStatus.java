package org.testin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Whether a package still holds current work. Persisted in the test set package
 * and test run package markers, which share the meaning and so share the enum.
 * <p>
 * Archived keeps everything inside it and changes only where it sits: an
 * archived package sorts after the active ones and does not expand with its
 * siblings, so last quarter's runs stop being the first thing in the tree (#68).
 */
@Getter
@AllArgsConstructor
public enum PackageStatus {
    ACTIVE(
            "Active",
            "Mark Active",
            "Sort this package with the current work"
    ),

    ARCHIVED(
            "Archived",
            "Archive",
            "Keep the contents, but sort the package last and leave it collapsed"
    );

    private final @NotNull String description;
    private final @NotNull String buttonName;
    private final @NotNull String buttonDescription;
}
