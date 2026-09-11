package org.testin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

/**
 * Whether a package still holds current work. Persisted in the test set package
 * and test run package markers, which share the meaning and so share the enum.
 * <p>
 * Archived keeps everything inside it and changes only how it is treated: the
 * package is retired ({@code DirectoryDto.isRetired()}) — drawn gray, ordered
 * after the active ones, left collapsed by expand-all, and its contents not
 * offered for a new run — so last quarter's runs stop being the first thing in
 * the tree (#68).
 */
@Getter
@AllArgsConstructor
public enum PackageStatus implements NodeStatus {
    ACTIVE(
            Bundle.message("status.package.active"),
            Bundle.message("status.package.active.action"),
            Bundle.message("status.package.active.description")
    ),

    ARCHIVED(
            Bundle.message("status.package.archived"),
            Bundle.message("status.package.archived.action"),
            Bundle.message("status.package.archived.description")
    );

    private final @NotNull String label;
    private final @NotNull String buttonName;
    private final @NotNull String buttonDescription;
}
