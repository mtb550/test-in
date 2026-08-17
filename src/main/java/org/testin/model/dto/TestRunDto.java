package org.testin.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;
import org.testin.model.TestRunItems;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString()
// todo, put uuid for each test run.
public class TestRunDto {

    @NotNull
    @Builder.Default
    private String changeLog = "";

    @NotNull
    @Builder.Default
    private String commitId = "";

    @NotNull
    @Builder.Default
    private String platform = "";

    @NotNull
    @Builder.Default
    private String component = "";

    @NotNull
    @Builder.Default
    private String language = "";

    @NotNull
    @Builder.Default
    private String browser = "";

    @NotNull
    @Builder.Default
    private String deviceType = "";

    @NotNull
    @Builder.Default
    private String testType = "";

    @NotNull
    @Builder.Default
    private String createdBy = "";

    @NotNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime createdAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    /**
     * When the tester first pressed Start Execution; {@link Config#NOT_EXECUTED}
     * until they do. Not {@code createdAt}: a run built in January and executed
     * in March is two different facts, and the reports used to print the first
     * under the heading of the second.
     */
    @NotNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime executionStartedAt = Config.NOT_EXECUTED;

    /**
     * When execution last stopped - the run completing, the tester pressing Stop,
     * or a verdict that ended the flow. {@link Config#NOT_EXECUTED} until then.
     */
    @NotNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime executionEndedAt = Config.NOT_EXECUTED;

    @NotNull
    @Builder.Default
    private List<TestRunItems> results = new ArrayList<>();

    /**
     * Stamps the first Start Execution press and keeps it. A tester who stops
     * halfway and resumes next week is continuing the same execution, so the run
     * still started when it started; only a run that has never been started takes
     * the stamp.
     */
    public void markExecutionStarted() {
        if (Config.isNotExecuted(executionStartedAt))
            executionStartedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * Every stop overwrites the previous one: the run ended when it last stopped.
     */
    public void markExecutionEnded() {
        executionEndedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

}