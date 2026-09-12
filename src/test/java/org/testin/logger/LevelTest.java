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

package org.testin.logger;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * The log's own vocabulary, which nothing else can check.
 * <p>
 * A level is two facts that have to agree with each other: how loud it is, and
 * the five characters it is printed as. The first decides what {@code Logger}
 * lets through, and the second is a column in a file people read by eye - a
 * level printed four characters wide moves every message on that line.
 */
public class LevelTest {

    /**
     * UC-INTERNAL-008, Rule-INTERNAL-063.
     * <p>
     * The log is read as columns, so the name is padded to the width of the
     * longest - which is five. A sixth level spelled longer than that has to
     * decide what the column is, rather than quietly widening one line.
     */
    @Test
    public void everyLevelIsPrintedTheSameWidth() {
        final @NotNull Set<Integer> widths = Arrays.stream(Level.values())
                .map(level -> level.paddedName.length())
                .collect(Collectors.toSet());

        assertEquals(widths, Set.of(5),
                "the levels are not all printed the same width, so a log line's columns no longer line up: "
                        + Arrays.stream(Level.values())
                        .map(level -> level.name() + "='" + level.paddedName + "'")
                        .collect(Collectors.joining(", ")));
    }

    /**
     * Loud to quiet is the declaration order, because {@code Logger} compares
     * priorities to decide what to write: a level declared out of order would
     * silently swallow the ones above it.
     */
    @Test
    public void loudnessRisesWithTheDeclarationOrder() {
        final @NotNull List<Level> levels = List.of(Level.values());

        for (int i = 1; i < levels.size(); i++) {
            assertTrue(levels.get(i).priority > levels.get(i - 1).priority,
                    levels.get(i) + " is not louder than " + levels.get(i - 1)
                            + ", so a tester who picks the quieter one is shown the louder one as well - or not at all");
        }
    }

    /**
     * Off is below every real level, which is what turns the log off rather than
     * choosing a volume for it.
     */
    @Test
    public void offIsQuieterThanAnythingThatWrites() {
        assertEquals(Level.DISABLED.priority, -1, "DISABLED is not below the levels it disables");

        assertTrue(Arrays.stream(Level.values())
                        .filter(level -> level != Level.DISABLED)
                        .allMatch(level -> level.priority >= 0),
                "a level that writes is at or below DISABLED, so turning the log off would not turn it off");
    }

    /**
     * UC-SETTING-007, Rule-SETTING-025.
     * <p>
     * A stored level that names nothing comes back INFO, because the reader is
     * {@code Level.valueOf} in the startup activity and it runs after the
     * once-per-project claim is spent: a word this enum does not know threw from
     * there, and the tester had an unindexed, empty tree for the session (#66,
     * finding 87).
     */
    @Test
    public void aStoredLevelThatNamesNothingIsRead() {
        for (final Level level : Level.values())
            assertEquals(Level.known(level.name()), level.name(), "a level this enum declares is kept as it was stored");

        assertEquals(Level.known("VERBOSE"), Level.INFO.name(), "a level from another build does not stop the project opening");
        assertEquals(Level.known("info"), Level.INFO.name(), "the stored form is the constant's own name, so case matters and a mismatch is unknown");
        assertEquals(Level.known(""), Level.INFO.name(), "nothing stored is nothing to parse");
    }
}
