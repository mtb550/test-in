package org.testin.model.markers;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.testin.model.Config;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * The audit block every marker carries: who created the node and when, and who
 * last modified it and when. Declared here once, so a marker class is only the
 * status it adds - two of them add nothing and are now empty.
 * <p>
 * Serialization lives here too. {@code ignoreUnknown} is what lets a marker
 * file written by an older build - or by a sibling marker that has a status
 * this one does not - still be read, and the date format is the one every
 * marker file on disk is already written in.
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
    private ZonedDateTime modifiedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
}
