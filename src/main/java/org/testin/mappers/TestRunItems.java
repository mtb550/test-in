package org.testin.mappers;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.BugPriority;
import org.testin.enums.BugSeverity;
import org.testin.enums.TestStatus;
import org.testin.mappers.dto.TestCaseDto;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestRunItems {

    /**
     * The test case this result belongs to, wired by the run editor after the
     * run JSON is read - {@code @JsonIgnore}, so it is never deserialised.
     * <p>
     * Null when the test case has been deleted since the run: the editor skips
     * those and never assigns one. They stay out of the list, so the attributes
     * that render a row can rely on it; a dialog opened for the raw run item
     * cannot.
     */
    @JsonIgnore
    @Nullable
    private TestCaseDto tc;

    @NotNull
    private UUID id;

    @NotNull
    @Builder.Default
    private TestStatus status = TestStatus.PENDING;

    @NotNull
    @Builder.Default
    private String actualResult = "";

    @NotNull
    @Builder.Default
    private BugSeverity bugSeverity = BugSeverity.EMPTY;

    @NotNull
    @Builder.Default
    private BugPriority bugPriority = BugPriority.EMPTY;

    @NotNull
    @Builder.Default
    private Duration duration = Duration.ZERO;

    @NotNull
    @Builder.Default
    private String executedBy = "";

    @NotNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime executedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    @NotNull
    @Builder.Default
    private String stacktrace = "";
}