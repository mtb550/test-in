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
import org.testin.util.TestDataParser;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class RunMarkerMerge {
    private static final @NotNull String STARTED = "executionStartedAt";
    private static final @NotNull String ENDED = "executionEndedAt";
    private static final @NotNull String STATUS = "status";
    private static final @NotNull String MODIFIED_AT = "modifiedAt";
    private static final @NotNull String MODIFIED_BY = "modifiedBy";

    private static final @NotNull List<String> WRITTEN_INTO = List.of("configuration", "resultAnalysis");

    // UC-SHARE-018, Rule-SHARE-080
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

    private static void execution(final @NotNull ObjectNode merged, final @NotNull ObjectNode mine, final @NotNull ObjectNode theirs, final @NotNull List<String> settled) {
        take(merged, mine, theirs, STARTED, true, settled, Bundle.message("git.merge.execution.started"));
        take(merged, mine, theirs, ENDED, false, settled, Bundle.message("git.merge.execution.ended"));
    }

    private static void take(final @NotNull ObjectNode merged, final @NotNull ObjectNode mine, final @NotNull ObjectNode theirs, final @NotNull String field, final boolean earliest, final @NotNull List<String> settled, final @NotNull String said) {
        final @NotNull ZonedDateTime ours = stamp(mine, field);
        final @NotNull ZonedDateTime yours = stamp(theirs, field);
        if (ours.equals(yours)) return;

        final boolean takeTheirs = Config.isNotExecuted(ours)
                || !Config.isNotExecuted(yours) && (earliest ? yours.isBefore(ours) : yours.isAfter(ours));

        if (takeTheirs) merged.set(field, theirs.path(field).deepCopy());
        settled.add(said);
    }

    private static void status(final @NotNull ObjectNode merged, final @NotNull ObjectNode mine, final @NotNull ObjectNode theirs, final @NotNull List<String> settled) {
        final @NotNull TestRunStatus ours = statusIn(mine);
        final @NotNull TestRunStatus yours = statusIn(theirs);
        if (ours == yours) return;

        if (yours.isFurtherThan(ours)) merged.set(STATUS, theirs.path(STATUS).deepCopy());
        settled.add(Bundle.message("git.merge.status"));
    }

    private static void audit(final @NotNull ObjectNode merged, final @NotNull ObjectNode mine, final @NotNull ObjectNode theirs) {
        if (!stamp(theirs, MODIFIED_AT).isAfter(stamp(mine, MODIFIED_AT))) return;

        merged.set(MODIFIED_AT, theirs.path(MODIFIED_AT).deepCopy());
        merged.set(MODIFIED_BY, theirs.path(MODIFIED_BY).deepCopy());
    }

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
                merged.withObjectProperty(object).set(key, yours.deepCopy());
                settled.add(Bundle.message("git.merge.written", FieldName.of(object + "." + key)));
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
        return TestDataParser.date(marker.path(field).asText("")).orElse(Config.NOT_EXECUTED);
    }
}
