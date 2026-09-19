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

package org.testin.git;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;
import org.testin.model.TestRunStatus;
import org.testin.util.Bundle;
import org.testin.util.Mapper;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Merges the three versions Git keeps of a test run's own marker (#305, Q-D).
 * <p>
 * Two testers executing one cycle both write its {@code .tr}: each Start stamps
 * the beginning, every Stop overwrites the end, and both move its status. Nothing
 * merged markers, so every shared run conflicted on this file - on values whose
 * disagreement has one obvious answer.
 * <p>
 * So it is answered by rule, and the tester is asked nothing about it: the run
 * started when the <b>earlier</b> of the two says it started, ended when the
 * <b>later</b> says it ended, and its status is the one <b>further along</b>,
 * because a run somebody completed is not created again. The audit block takes
 * the later edit, as a test case's does.
 * <p>
 * What the two testers wrote - the configuration and the result analysis - is
 * merged key by key, and only a key both of them changed differently is a
 * question. Everything else about the marker is the file's own: its id, when it
 * was created and by whom, and where it sits among its siblings.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RunMarkerMerge {

    private static final @NotNull DateTimeFormatter WRITTEN = DateTimeFormatter.ofPattern(Config.DATE_FORMAT_PATTERN, Locale.US);

    private static final @NotNull String STARTED = "executionStartedAt";
    private static final @NotNull String ENDED = "executionEndedAt";
    private static final @NotNull String STATUS = "status";
    private static final @NotNull String MODIFIED_AT = "modifiedAt";
    private static final @NotNull String MODIFIED_BY = "modifiedBy";

    /**
     * The two maps a tester writes into, which merge key by key rather than as
     * one value: two testers who answered different questions, or wrote about
     * different verdicts, disagreed about nothing.
     */
    private static final @NotNull List<String> WRITTEN_INTO = List.of("configuration", "resultAnalysis");

    /**
     * UC-SHARE-018, Rule-SHARE-080.
     * <p>
     * The three stages Git holds, by the rules above.
     */
    public static @NotNull Merge of(final @NotNull Mapper mapper, final @NotNull String base, final @NotNull String mine, final @NotNull String theirs) {
        final @NotNull ObjectNode baseNode = mapper.readTree(base);
        final @NotNull ObjectNode mineNode = mapper.readTree(mine);
        final @NotNull ObjectNode theirsNode = mapper.readTree(theirs);

        final @NotNull ObjectNode merged = mineNode.deepCopy();
        final @NotNull List<Merge.Question> questions = new ArrayList<>();
        final @NotNull List<String> settled = new ArrayList<>();

        execution(merged, mineNode, theirsNode, settled);
        status(merged, mineNode, theirsNode, settled);
        audit(merged, mineNode, theirsNode);

        for (final String object : WRITTEN_INTO) {
            written(mapper, merged, baseNode, mineNode, theirsNode, object, questions, settled);
        }

        return new Merge(merged, List.copyOf(questions), List.copyOf(settled));
    }

    /**
     * The run started when the earlier side says it started and ended when the
     * later says it ended - a cycle two people executed ran from the first thing
     * either of them did to the last.
     */
    private static void execution(final @NotNull ObjectNode merged, final @NotNull ObjectNode mine, final @NotNull ObjectNode theirs, final @NotNull List<String> settled) {
        take(merged, mine, theirs, STARTED, true, settled, Bundle.message("git.merge.execution.started"));
        take(merged, mine, theirs, ENDED, false, settled, Bundle.message("git.merge.execution.ended"));
    }

    private static void take(final @NotNull ObjectNode merged, final @NotNull ObjectNode mine, final @NotNull ObjectNode theirs, final @NotNull String field, final boolean earliest, final @NotNull List<String> settled, final @NotNull String said) {
        final @NotNull ZonedDateTime ours = stamp(mine, field);
        final @NotNull ZonedDateTime yours = stamp(theirs, field);
        if (ours.equals(yours)) return;

        // A stamp nobody made is not a candidate: the epoch means it never
        // happened, so the side that did happen wins whichever end this is.
        final boolean takeTheirs = Config.isNotExecuted(ours)
                || !Config.isNotExecuted(yours) && (earliest ? yours.isBefore(ours) : yours.isAfter(ours));

        if (takeTheirs) merged.set(field, theirs.path(field).deepCopy());
        settled.add(said);
    }

    /**
     * The status further along: a run somebody has completed is not created
     * again, and one somebody closed is not in progress.
     */
    private static void status(final @NotNull ObjectNode merged, final @NotNull ObjectNode mine, final @NotNull ObjectNode theirs, final @NotNull List<String> settled) {
        final @NotNull TestRunStatus ours = statusIn(mine);
        final @NotNull TestRunStatus yours = statusIn(theirs);
        if (ours == yours) return;

        if (yours.ordinal() > ours.ordinal()) merged.set(STATUS, theirs.path(STATUS).deepCopy());
        settled.add(Bundle.message("git.merge.status"));
    }

    /**
     * The audit block takes the later edit whole, so who last changed the run and
     * when stay one fact - the same rule a test case's takes.
     */
    private static void audit(final @NotNull ObjectNode merged, final @NotNull ObjectNode mine, final @NotNull ObjectNode theirs) {
        if (!stamp(theirs, MODIFIED_AT).isAfter(stamp(mine, MODIFIED_AT))) return;

        merged.set(MODIFIED_AT, theirs.path(MODIFIED_AT).deepCopy());
        merged.set(MODIFIED_BY, theirs.path(MODIFIED_BY).deepCopy());
    }

    /**
     * What the testers wrote, key by key: a key one side left alone takes the
     * other's, and only a key both changed differently is a question - named as
     * {@code configuration.PLATFORM}, which is what {@link Merge#answer} writes
     * back into.
     */
    private static void written(final @NotNull Mapper mapper, final @NotNull ObjectNode merged, final @NotNull ObjectNode base, final @NotNull ObjectNode mine, final @NotNull ObjectNode theirs, final @NotNull String object, final @NotNull List<Merge.Question> questions, final @NotNull List<String> settled) {
        final @NotNull Set<String> keys = new LinkedHashSet<>();
        mine.path(object).fieldNames().forEachRemaining(keys::add);
        theirs.path(object).fieldNames().forEachRemaining(keys::add);

        for (final String key : keys) {
            final @NotNull JsonNode was = base.path(object).path(key);
            final @NotNull JsonNode ours = mine.path(object).path(key);
            final @NotNull JsonNode yours = theirs.path(object).path(key);

            if (ours.equals(yours)) continue;

            if (ours.equals(was)) {
                merged.with(object).set(key, yours.deepCopy());
                settled.add(object + "." + key);
                continue;
            }
            if (yours.equals(was)) continue;

            questions.add(new Merge.Question(object + "." + key, ours.asText(""), yours.asText("")));
        }
    }

    private static @NotNull TestRunStatus statusIn(final @NotNull ObjectNode marker) {
        try {
            return TestRunStatus.valueOf(marker.path(STATUS).asText(TestRunStatus.CREATED.name()));
        } catch (final IllegalArgumentException unknown) {
            return TestRunStatus.CREATED;
        }
    }

    private static @NotNull ZonedDateTime stamp(final @NotNull ObjectNode marker, final @NotNull String field) {
        final @NotNull String written = marker.path(field).asText("");
        if (written.isBlank()) return Config.NOT_EXECUTED;

        try {
            return ZonedDateTime.parse(written, WRITTEN);
        } catch (final RuntimeException notADate) {
            return Config.NOT_EXECUTED;
        }
    }
}
