package org.testin.mappers.markers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * The contract every directory marker shares. The marker JSON file is the only
 * place a node's audit info (who created/modified it, and when) is stored —
 * the in-memory DTOs carry none of it. Lombok's {@code @Getter}/{@code @Setter}
 * with {@code @Accessors(chain = true)} on the marker classes generate these
 * methods, so implementing the interface costs nothing.
 */
public interface IMarker {

    @NotNull String getCreatedBy();

    @NotNull ZonedDateTime getCreatedAt();

    @NotNull String getModifiedBy();

    @NotNull ZonedDateTime getModifiedAt();

    IMarker setCreatedBy(@NotNull String createdBy);

    IMarker setCreatedAt(@NotNull ZonedDateTime createdAt);

    IMarker setModifiedBy(@NotNull String modifiedBy);

    IMarker setModifiedAt(@NotNull ZonedDateTime modifiedAt);

    /** Fills the creation audit info; modified mirrors created at birth. */
    default void stampCreated(final @NotNull String tester) {
        final ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        setCreatedBy(tester);
        setCreatedAt(now);
        setModifiedBy(tester);
        setModifiedAt(now);
    }

    /** Records a modification by the given tester, now. */
    default void touch(final @NotNull String tester) {
        setModifiedBy(tester);
        setModifiedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

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
