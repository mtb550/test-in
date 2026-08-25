package org.testin.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;
import org.testin.model.ResultAnalysis;
import org.testin.model.TestRunItems;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
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

    /**
     * What the tester wrote about each verdict after the run finished, under the
     * verdict it is about (#3. Result Analysis in the reports).
     * <p>
     * On the run rather than beside it, because it is a fact about this run and
     * travels with it - the reports read it, a colleague pulling the run reads
     * it, and it is committed with the results it explains.
     * <p>
     * A map keyed by the section rather than four fields: the sections, their
     * headings and their counts all belong to {@link ResultAnalysis}, so a fifth
     * one would be written, shown and reported without this class changing. Left
     * out of the file entirely when nothing was written.
     */
    @NotNull
    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<ResultAnalysis, String> resultAnalysis = new EnumMap<>(ResultAnalysis.class);

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
     * Every stop overwrites the previous one: the run ended when it last stopped,
     * or when it reached a terminal status, from the editor or from the tree.
     * A run that was never started has no end - this is the one place that
     * knows so, and every caller stays unconditional.
     */
    public void markExecutionEnded() {
        if (Config.isNotExecuted(executionStartedAt)) return;

        executionEndedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * Clears the execution stamp on every case that never received a verdict.
     * <p>
     * Runs built before {@code 68fc9994} defaulted executedAt to the moment the
     * run was created, so every pending case in those files carries a plausible
     * execution time, and the Executed At column shows it as though someone had
     * run the case. {@code TestRunItems.recordVerdict} is the only writer of that
     * field and is only reached with a verdict, so a time on a case at any other
     * status can only be the old default and never a real stamp.
     * <p>
     * Applied once, where the run is read, so nothing downstream has to know a
     * file might predate the fix. The file itself heals whenever the run is next
     * written; nothing is rewritten just for having been opened.
     */
    public void dropStampsWithoutVerdict() {
        for (final TestRunItems item : results) {
            // REMOVED is not a verdict either, and its stamp still stands: the
            // case was executed before it was deleted, so the time is real
            // rather than the old default this repair exists to clear.
            if (!item.getStatus().isVerdict() && !item.isRemoved()) {
                item.setExecutedAt(Config.NOT_EXECUTED);
            }
        }
    }

}