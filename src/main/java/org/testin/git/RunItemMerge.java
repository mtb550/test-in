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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;
import org.testin.util.Bundle;
import org.testin.util.Mapper;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Merges the three versions Git keeps of one case's result (#305, Q-E).
 * <p>
 * <b>The later verdict wins whole.</b> A result is one tester's account of
 * executing one case: the verdict, when they gave it, how long it took, what
 * they saw, the stacktrace, the screenshots and what they filed about it. Those
 * travel together or they say something nobody recorded - a Passed carrying the
 * other tester's stacktrace, a Failed with no actual result.
 * <p>
 * So this asks nothing. The side whose {@code executedAt} is later is the side
 * kept, and the tester is told a choice was made, which is what
 * {@link Merge#settled} is for. Two testers judging different cases never reach
 * here at all: their verdicts are in different files now, which is the reason
 * this story exists.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RunItemMerge {

    private static final @NotNull DateTimeFormatter WRITTEN = DateTimeFormatter.ofPattern(Config.DATE_FORMAT_PATTERN, Locale.US);

    private static final @NotNull String EXECUTED_AT = "executedAt";

    /**
     * UC-SHARE-018, Rule-SHARE-080.
     * <p>
     * The three stages Git holds, as one verdict.
     *
     * @param base   the common ancestor, unread: a verdict is not merged field by
     *               field, so what it grew from decides nothing
     * @param mine   this machine's version
     * @param theirs the version the pull brought
     */
    public static @NotNull Merge of(final @NotNull Mapper mapper, final @NotNull String base, final @NotNull String mine, final @NotNull String theirs) {
        final @NotNull ObjectNode mineNode = mapper.readTree(mine);
        final @NotNull ObjectNode theirsNode = mapper.readTree(theirs);

        if (mineNode.equals(theirsNode)) return new Merge(mineNode, List.of(), List.of());

        final boolean takeTheirs = executedAt(theirsNode).isAfter(executedAt(mineNode));

        return new Merge(takeTheirs ? theirsNode : mineNode, List.of(), List.of(Bundle.message("git.merge.verdict")));
    }

    /**
     * When the verdict was given, and the epoch for a result nobody executed -
     * which is what a pending result carries, and what a file written by
     * something else may leave out entirely.
     */
    private static @NotNull ZonedDateTime executedAt(final @NotNull ObjectNode item) {
        final @NotNull String written = item.path(EXECUTED_AT).asText("");
        if (written.isBlank()) return Config.NOT_EXECUTED;

        try {
            return ZonedDateTime.parse(written, WRITTEN);
        } catch (final RuntimeException notADate) {
            return Config.NOT_EXECUTED;
        }
    }
}
