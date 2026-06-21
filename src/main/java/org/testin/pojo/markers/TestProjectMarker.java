package org.testin.pojo.markers;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.Accessors;
import org.testin.pojo.Config;
import org.testin.pojo.ProjectStatus;

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
public class TestProjectMarker {
    @Builder.Default
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @NonNull
    @Builder.Default
    private String createdBy = "";

    @NonNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime createdAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

}
