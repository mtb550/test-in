package org.testin.model.markers;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;
import org.testin.model.TestSetStatus;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString()
public class TestSetMarker implements Marker {
    /**
     * Deprecated test sets keep their cases and their run history; they stop
     * being offered when a new run is configured (#68).
     */
    @NonNull
    @Builder.Default
    private TestSetStatus status = TestSetStatus.ACTIVE;

    @NonNull
    @Builder.Default
    private String createdBy = "";

    @NonNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime createdAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    @NonNull
    @Builder.Default
    private String modifiedBy = "";

    @NonNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime modifiedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Override
    public @NotNull String getStatusLabel() {
        return status.getLabel();
    }
}
