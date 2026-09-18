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

package org.testin.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;
import org.testin.model.ResultAnalysis;
import org.testin.model.TestRunConfiguration;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
    @NotNull
    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<TestRunConfiguration, String> configuration = new EnumMap<>(TestRunConfiguration.class);

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
     * The same run, covering exactly these cases (#96).
     * <p>
     * A case that stays keeps its result <b>whole</b> - the verdict, the actual
     * result, the bug severity and priority, the duration, who executed it and
     * when, and the stack trace - because the item is carried across rather than
     * rebuilt from its id. A case that arrives is {@link TestStatus#PENDING}. A
     * case that goes is dropped with everything it recorded.
     * <p>
     * Order is the run's own and is left alone: the cases it already covered stay
     * in the order it had them, and new ones are appended. Re-covering a run is
     * not a re-sort.
     * <p>
     * Returns a new run and leaves this one untouched, so the caller still holds
     * what the run was - which is what an undo puts back.
     */
    public @NotNull TestRunDto coverOnly(final @NotNull Set<UUID> wanted) {
        final @NotNull Map<UUID, TestRunItems> held = new LinkedHashMap<>();
        results.forEach(item -> held.put(item.getId(), item));

        final @NotNull List<TestRunItems> covered = new ArrayList<>();
        held.forEach((id, item) -> {
            if (wanted.contains(id)) covered.add(item);
        });

        wanted.stream()
                .filter(id -> !held.containsKey(id))
                .forEach(id -> covered.add(new TestRunItems().setId(id).setStatus(TestStatus.PENDING)));

        return new TestRunDto()
                .setConfiguration(configuration)
                .setResultAnalysis(resultAnalysis)
                .setExecutionStartedAt(executionStartedAt)
                .setExecutionEndedAt(executionEndedAt)
                .setResults(covered);
    }

    /**
     * Whether every case in this run has been judged.
     * <p>
     * Asked of the run rather than counted at a call site, because the answer
     * decides when a run is over and two places counting it would eventually
     * disagree about a deleted case.
     * <p>
     * A run with no cases is not finished, it is empty - completing it the
     * moment it is created would be the wrong answer to a question nobody
     * asked.
     */
    @JsonIgnore
    public boolean isFullyJudged() {
        return !results.isEmpty() && results.stream().allMatch(TestRunItems::isJudged);
    }

    /**
     * This run's result for one test case, whatever state it is in, and empty
     * when the run does not cover the test case. Every lookup by id asks here.
     */
    public @NotNull Optional<TestRunItems> resultOf(final @NotNull UUID testCaseId) {
        return results.stream().filter(item -> item.getId().equals(testCaseId)).findFirst();
    }

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

}