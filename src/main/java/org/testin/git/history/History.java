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
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.List;

public record History(@NotNull List<HistoryEntry> entries, @NotNull List<BugCard> bugs, @NotNull String problem) {
    // Rule-VIEW-PANEL-100
    public static final @NotNull History NOT_UNDER_GIT = new History(List.of(), List.of(), Bundle.message("view.history.not.under.git"));

    static @NotNull History read(final @NotNull List<HistoryEntry> entries) {
        return new History(List.copyOf(entries), List.of(), "");
    }

    public static @NotNull History failed(final @NotNull String reason) {
        return new History(List.of(), List.of(), Bundle.message("view.history.failed", reason));
    }

    // Rule-VIEW-PANEL-106
    public @NotNull History with(final @NotNull List<BugCard> bugCards) {
        return new History(entries, List.copyOf(bugCards), problem);
    }

    // Rule-VIEW-PANEL-105, Rule-VIEW-PANEL-107
    public @NotNull List<HistoryCard> cards() {
        final @NotNull List<HistoryCard> cards = new ArrayList<>();
        int entry = 0;
        int bug = 0;

        while (entry < entries.size() || bug < bugs.size()) {
            if (bug == bugs.size() || entry < entries.size() && !isNewer(bugs.get(bug), entries.get(entry))) {
                cards.add(entries.get(entry++));
            } else {
                cards.add(bugs.get(bug++));
            }
        }
        return cards;
    }

    private static boolean isNewer(final @NotNull BugCard bug, final @NotNull HistoryEntry entry) {
        if (bug.hash().equals(entry.hash())) return false;
        if (bug.isCommitted() != entry.isCommitted()) return !bug.isCommitted();

        return bug.when().isAfter(entry.when());
    }
}
