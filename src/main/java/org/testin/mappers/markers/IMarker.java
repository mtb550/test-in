package org.testin.mappers.markers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    /**
     * Human-readable status of the node, or null when the marker carries none.
     * JsonIgnore everywhere: a derived label must never leak into the
     * persisted marker JSON.
     */
    @JsonIgnore
    default @Nullable String getStatusLabel() {
        return null;
    }
}
