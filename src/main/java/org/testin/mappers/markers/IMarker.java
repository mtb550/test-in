package org.testin.mappers.markers;

import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;

/**
 * The contract every directory marker shares. Lombok's {@code @Getter} on the
 * marker classes generates these methods, so implementing the interface costs
 * nothing — and {@code DirectoryDto#getMarker()} can be abstract with each
 * subtype returning its concrete marker via covariant return.
 */
public interface IMarker {

    @NotNull String getCreatedBy();

    @NotNull ZonedDateTime getCreatedAt();
}
