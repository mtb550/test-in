package org.testin.model.markers;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * The audit block every marker carries: who created the node and when, and who
 * last modified it and when. Declared here once, so a marker class is only the
 * status it adds - two of them add nothing and are now empty.
 * <p>
 * Serialization lives here too. {@code ignoreUnknown} is what lets a marker
 * file written by an older build still be read - or one written by a sibling
 * marker that has a status this one does not.
 * <p>
 * The date format is the one every marker file on disk is already written in.
 * <p>
 * The {@code updatedBy}/{@code updatedAt} aliases are the pre-rename keys.
 * Only the test project marker declared them, because it was the only marker
 * that existed when the fields were renamed; now that the block has one owner,
 * every marker reads an old file rather than the one that happened to be
 * patched (#66).
 */
@Setter
@Getter
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public abstract class AbstractMarker implements Marker {

    /**
     * Where this node sits among its siblings: a number the tester typed.
     * <p>
     * {@link Marker#NOT_ORDERED} when they have not, which is the largest number
     * there is - so an unnumbered node sorts after every numbered one by
     * ordinary comparison, and nothing anywhere has to ask whether a number is
     * really a number. A marker file written before this existed has no such key
     * and arrives here the same way.
     * <p>
     * Left out of the JSON when it is that value, so a file nobody ordered says
     * nothing about order rather than carrying a number no human would write.
     * <p>
     * Here rather than on each kind of node: a test set, a package and a project
     * all sit among siblings, and one field they inherit is one place to change
     * it - the same reason the audit pair below lives here.
     */
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = AbstractMarker.Unordered.class)
    private int order = Marker.NOT_ORDERED;

    /**
     * Keeps {@link Marker#NOT_ORDERED} out of the file.
     * <p>
     * Jackson asks a value filter whether it {@code equals} the value being
     * written and leaves the key out when it says yes - which is why this class
     * answers a question about a number it is not. That is the contract Jackson
     * documents for {@code JsonInclude.Include.CUSTOM}, and the reason to use it
     * here is that the alternative is a marker carrying 2147483647: a number no
     * human wrote, in a file people read and commit.
     */
    static final class Unordered {

        @Override
        public boolean equals(final Object value) {
            return value instanceof Integer order && order == Marker.NOT_ORDERED;
        }

        @Override
        public int hashCode() {
            return Marker.NOT_ORDERED;
        }
    }

    @NonNull
    private String createdBy = "";

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime createdAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    @JsonAlias("updatedBy")
    @NonNull
    private String modifiedBy = "";

    @JsonAlias("updatedAt")
    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime modifiedAt = Config.NOT_EXECUTED;

    /**
     * Who last modified the node - its creator, until somebody else does.
     * <p>
     * Markers written before these two fields existed carry only the creation
     * pair, and a node that was never modified was last touched when it was
     * made. The default used to be {@code now()}, so those nodes reported
     * themselves as modified at the moment they were read, and the Details popup
     * printed today's date for a directory nobody had touched in months.
     * <p>
     * Answered here rather than at each reader, so the popup, the reports and
     * whatever asks next stay unconditional - and what is written back is the
     * same answer, not the invented one.
     */
    public @NotNull String getModifiedBy() {
        return modifiedBy.isBlank() ? createdBy : modifiedBy;
    }

    /**
     * When the node was last modified, which is when it was created until it is.
     * See {@link #getModifiedBy()} for why the pair answers this way.
     */
    public @NotNull ZonedDateTime getModifiedAt() {
        return Config.isNotExecuted(modifiedAt) ? createdAt : modifiedAt;
    }
}
