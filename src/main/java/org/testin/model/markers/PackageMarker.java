package org.testin.model.markers;

import org.jetbrains.annotations.NotNull;
import org.testin.model.PackageStatus;

/**
 * A marker that carries a {@link PackageStatus}: the test set package and the
 * test run package. The two markers are separate classes because they are
 * separate files with separate names on disk, but archiving means the same
 * thing to both, so one action sets it through this and never asks which
 * package it is holding.
 */
public interface PackageMarker extends Marker {

    @NotNull PackageStatus getStatus();

    PackageMarker setStatus(@NotNull PackageStatus status);
}
