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
import org.testin.git.change.FieldChange;

import java.time.ZonedDateTime;
import java.util.List;

public record HistoryEntry(@NotNull HistoryEntryKind kind, @NotNull String hash, @NotNull String who, @NotNull ZonedDateTime when, @NotNull String message, @NotNull List<FieldChange> changes) {
    private static final int SHORT_HASH = 7;

    static @NotNull HistoryEntry of(final @NotNull HistoryEntryKind kind, final @NotNull HistoryCommit commit, final @NotNull List<FieldChange> changes) {
        return new HistoryEntry(kind, commit.hash(), commit.who(), commit.when(), commit.message(), changes);
    }

    // Rule-VIEW-PANEL-098
    public boolean isCommitted() {
        return !hash.isEmpty();
    }

    // Rule-VIEW-PANEL-097
    public @NotNull String shortHash() {
        return hash.substring(0, Math.min(SHORT_HASH, hash.length()));
    }
}
