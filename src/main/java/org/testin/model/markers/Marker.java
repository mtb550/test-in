package org.testin.model.markers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.time.temporal.ChronoUnit;

/**
 * The contract every directory marker shares. The marker JSON file is the only
 * place a node's audit info (who created/modified it, and when) is stored —
 * the in-memory DTOs carry none of it. Lombok's {@code @Getter}/{@code @Setter}
 * with {@code @Accessors(chain = true)} on the marker classes generate these
 * methods, so implementing the interface costs nothing.
 */
// UnusedReturnValue reports all four setters, and none of them can be void: the
// marker classes use Lombok @Accessors(chain = true), whose generated setters
// return the concrete marker type, and a return type is not covariant with void.
// The chaining is the reason the interface exists, even where the default methods
// below discard it (#66, C3).
@SuppressWarnings("UnusedReturnValue")
public interface Marker {

    /**
     * What a node's order is when nobody has given it one: the largest number
     * there is, so it sorts after everything a tester did number without any
     * reader having to notice that it means "none".
     * <p>
     * The alternative was zero, which is what an {@code int} defaults to - and
     * zero sorts first in an ascending sort, which is the opposite of what it
     * means. Every reader would then carry the correction. This way the value
     * carries it, once.
     */
    int NOT_ORDERED = Integer.MAX_VALUE;

    /**
     * Where the node sits among its siblings, {@link #NOT_ORDERED} when nobody
     * has said - see {@link org.testin.model.dto.dirs.DirectoryDto#getOrder}.
     */
    int getOrder();

    @NotNull Marker setOrder(int order);

    @NotNull String getCreatedBy();

    Marker setCreatedBy(@NotNull String createdBy);

    @NotNull ZonedDateTime getCreatedAt();

    Marker setCreatedAt(@NotNull ZonedDateTime createdAt);

    @NotNull String getModifiedBy();

    Marker setModifiedBy(@NotNull String modifiedBy);

    @NotNull ZonedDateTime getModifiedAt();

    Marker setModifiedAt(@NotNull ZonedDateTime modifiedAt);

    /**
     * Fills the creation audit info; modified mirrors created at birth.
     */
    default void stampCreated(final @NotNull String tester) {
        final @NotNull ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        setCreatedBy(tester);
        setCreatedAt(now);
        setModifiedBy(tester);
        setModifiedAt(now);
    }

    /**
     * Records a modification by the given tester, now.
     */
    default void touch(final @NotNull String tester) {
        setModifiedBy(tester);
        setModifiedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    /**
     * Human-readable status of the node; empty when the marker carries none.
     * Empty rather than null because the details popup drops a blank row
     * anyway, so no reader has to ask whether this marker has a status.
     * JsonIgnore everywhere: a derived label must never leak into the
     * persisted marker JSON.
     */
    @JsonIgnore
    default @NotNull String getStatusLabel() {
        return "";
    }

    /**
     * What else this marker has to say about its node, as captions and values
     * the Details popup lists under the audit block.
     * <p>
     * Empty for a marker with nothing to add, which is most of them. The same
     * shape as {@link #getStatusLabel()} and for the same reason: the popup adds
     * whatever comes back without asking which kind of marker it is, and the
     * details builder drops a row whose value is blank.
     * <p>
     * JsonIgnore for the reason the status label is - a derived list must never
     * reach the marker file.
     */
    @JsonIgnore
    default @NotNull List<DetailRow> getDetailRows() {
        return List.of();
    }
}
