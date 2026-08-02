package org.testin.mappers;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.Priority;
import org.testin.enums.Severity;
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

    @JsonIgnore
    @NotNull
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
    private Severity severity = Severity.MINOR;

    @NotNull
    @Builder.Default
    private Priority Priority = org.testin.enums.Priority.LOW;

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