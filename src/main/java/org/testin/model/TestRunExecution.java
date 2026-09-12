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

package org.testin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestRunDto;
import org.testin.model.markers.DetailRow;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * What a run recorded about its own execution, and what each of those is called.
 * <p>
 * The counterpart of {@link TestRunConfiguration}, which owns the fields a
 * tester answered when the run was created. These are the two the run wrote for
 * itself, and they are declared the same way for the same reason: three surfaces
 * show them - the Details popup, the report overview, and the Git review naming
 * what changed - and each used to spell the caption out and format the date for
 * itself. Three copies of one label is two chances to stop agreeing, and the one
 * that drifts is the one nobody notices, because each reads correctly alone.
 * <p>
 * An enum rather than constants on {@link TestRunDto}, which is where they were
 * first written and where they did not belong: a DTO carries what the run
 * stores, and a caption is not stored. Naming a value and reading it are one
 * job, so they sit on one thing - the same shape {@code RunEditorAttributes}
 * uses for what a run row has to say.
 */
@Getter
@AllArgsConstructor
public enum TestRunExecution {

    STARTED(
            Bundle.message("execution.started"),
            TestRunDto::getExecutionStartedAt
    ),

    ENDED(
            Bundle.message("execution.ended"),
            TestRunDto::getExecutionEndedAt
    );

    private final @NotNull String displayName;

    /**
     * Not exposed: what a caller wants is the value to show, and handing out the
     * timestamp would let each of them format it differently - which is half of
     * what this exists to stop.
     */
    @Getter(lombok.AccessLevel.NONE)
    private final @NotNull Function<TestRunDto, ZonedDateTime> at;

    /**
     * This field of that run, formatted, and blank when the run never reached
     * it. Every reader here drops a blank row, so a run nobody executed shows
     * none of these rather than empty ones.
     */
    public @NotNull String valueIn(final @NotNull TestRunDto run) {
        return Display.formatDate(at.apply(run));
    }

    /**
     * What the run took, blank until it has both ends.
     * <p>
     * Not a constant of this enum, and that is the point: the two above are
     * what the run wrote down, and this is arithmetic on them. As a constant it
     * would reach the Git review through {@code values()}, and one run
     * finishing would be reported as three fields changing - started, ended,
     * and the number derived from both.
     */
    private static @NotNull String tookIn(final @NotNull TestRunDto run) {
        final @NotNull ZonedDateTime from = run.getExecutionStartedAt();
        final @NotNull ZonedDateTime to = run.getExecutionEndedAt();

        if (Config.isNotExecuted(from) || Config.isNotExecuted(to)) return "";

        return Display.formatRunClock(Duration.between(from, to));
    }

    /**
     * All of them, as rows, in declaration order - which is the order they
     * happened in - and then how long the run took.
     * <p>
     * Every reader here drops a blank row, so a run nobody executed shows none
     * of these rather than three empty ones.
     */
    public static @NotNull List<DetailRow> rowsOf(final @NotNull TestRunDto run) {
        return Stream.concat(
                        Arrays.stream(values()).map(field -> new DetailRow(field.displayName, field.valueIn(run))),
                        Stream.of(new DetailRow(Bundle.message("execution.time"), tookIn(run))))
                .toList();
    }
}
