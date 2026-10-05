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

package org.testin.git.history;

import org.jetbrains.annotations.NotNull;
import org.testin.model.result.TestRunItems;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class HistoryCardsTest {

    private static @NotNull ZonedDateTime on(final int day) {
        return ZonedDateTime.parse("2026-10-0%dT16:40:00+03:00".formatted(day));
    }

    private static @NotNull HistoryEntry entry(final @NotNull String hash, final int day) {
        return new HistoryEntry(HistoryEntryKind.CHANGED, hash, "Sara", on(day), "", List.of());
    }

    private static @NotNull BugCard bug(final @NotNull String hash, final int day) {
        return new BugCard(hash, "Sara", on(day), new BugEvent(BugEventKind.RECORDED, Path.of("Cycle 3"), TestRunItems.builder().build(), List.of()));
    }

    // Rule-VIEW-PANEL-105, Rule-VIEW-PANEL-107
    @Test
    public void cardsAreNewestFirstWithWhatIsNotCommittedOnTopAndTheTestCaseCardFirstWithinOneCommit() {
        final @NotNull HistoryEntry uncommitted = entry("", 1);
        final @NotNull HistoryEntry review = entry("c".repeat(40), 4);
        final @NotNull HistoryEntry created = entry("a".repeat(40), 1);
        final @NotNull BugCard uncommittedBug = bug("", 5);
        final @NotNull BugCard sameCommit = bug("c".repeat(40), 4);
        final @NotNull BugCard between = bug("b".repeat(40), 2);

        final @NotNull History history = History.read(List.of(uncommitted, review, created)).with(List.of(uncommittedBug, sameCommit, between));

        assertEquals(history.cards(), List.of(uncommitted, uncommittedBug, review, sameCommit, between, created));
    }

    // Rule-VIEW-PANEL-100
    @Test
    public void aTestProjectNotUnderGitKeepsItsLineBesideTheBugsItHoldsNow() {
        final @NotNull History history = History.NOT_UNDER_GIT.with(List.of(bug("", 5)));

        assertEquals(history.cards().size(), 1);
        assertEquals(history.problem(), History.NOT_UNDER_GIT.problem());
    }
}
