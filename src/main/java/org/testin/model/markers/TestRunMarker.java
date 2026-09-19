/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.model.markers;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;
import org.testin.model.ResultAnalysis;
import org.testin.model.TestRunConfiguration;
import org.testin.model.TestRunExecution;
import org.testin.model.TestRunStatus;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


@Setter
@Getter
@Accessors(chain = true)
@ToString(callSuper = true)
public class TestRunMarker extends AbstractMarker {
    @NonNull
    private TestRunStatus status = TestRunStatus.CREATED;

    /**
     * What the tester answered when the run was created, under the question it
     * answers.
     * <p>
     * A map keyed by {@link TestRunConfiguration} rather than eight named
     * fields, for the reason {@link #resultAnalysis} below is one: the
     * questions, their captions, the rule for when each applies and how each is
     * read all belong to that enum, so a ninth is asked, stored, compared and
     * reported by declaring it there and nowhere else.
     * <p>
     * Eight fields could not do that, and had already failed to. The report
     * overview listed five of them, so the language, the browser and the device
     * type were asked of the tester, written to two files, and printed in no
     * report at all; the Git comparator listed all eight directly above a loop
     * that walks a sibling enum. Both are loops now, over this.
     * <p>
     * Only what was answered is stored, so a run left with the defaults writes
     * no configuration at all rather than eight empty strings - and a question
     * that did not apply, a browser on a mobile run, is absent rather than
     * blank.
     */
    @NonNull
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<TestRunConfiguration, String> configuration = new EnumMap<>(TestRunConfiguration.class);

    /**
     * What the tester wrote about each verdict after the run finished, under the
     * verdict it is about (#3. Result Analysis in the reports).
     * <p>
     * In the run's own marker, because it is a fact about this run and travels
     * with it - the reports read it, a colleague pulling the run reads
     * it, and it is committed with the results it explains.
     * <p>
     * A map keyed by the section rather than four fields: the sections, their
     * headings and their counts all belong to {@link ResultAnalysis}, so a fifth
     * one would be written, shown and reported without this marker changing. Left
     * out of the file entirely when nothing was written.
     */
    @NonNull
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<ResultAnalysis, String> resultAnalysis = new EnumMap<>(ResultAnalysis.class);

    /**
     * When the tester first pressed Start Execution; {@link Config#NOT_EXECUTED}
     * until they do. Not {@code createdAt}: a run built in January and executed
     * in March is two different facts, and the reports used to print the first
     * under the heading of the second.
     */
    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime executionStartedAt = Config.NOT_EXECUTED;

    /**
     * When execution last stopped - the run completing, the tester pressing Stop,
     * or a verdict that ended the flow. {@link Config#NOT_EXECUTED} until then.
     */
    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime executionEndedAt = Config.NOT_EXECUTED;

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
     * UC-INTERNAL-007, Rule-INTERNAL-075.
     * <p>
     * What this run has to say about itself, beside the audit block every marker
     * shows: when it was executed and how long it took, and the answers the
     * tester gave when it was created. The details dialog asks the marker rather
     * than asking the index for the run and naming the two sets itself, which is
     * the same reason the status row arrives this way (#305, S15).
     */
    @JsonIgnore
    @Override
    public @NotNull List<DetailRow> getDetailRows() {
        return Stream.concat(TestRunExecution.rowsOf(this).stream(), TestRunConfiguration.rowsOf(this).stream()).toList();
    }

    /**
     * The one marker that shows a status the tester cannot set.
     * <p>
     * A run's status is the run's own account of itself - created, executed -
     * moved by running it, not by a menu entry. So {@link TestRunStatus} is not
     * a {@link org.testin.model.NodeStatus} and this answers the label directly
     * rather than through {@code status()} (#110).
     */
    @JsonIgnore
    @Override
    public @NotNull String getStatusLabel() {
        return status.getLabel();
    }

}
