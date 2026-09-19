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

import org.testin.model.TestRunConfiguration;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunExecution;

import java.util.*;
import org.testin.model.markers.TestRunMarker;

/**
 * Compares two revisions of a test run's own facts - how it was configured and
 * when it was executed, which its {@code .tr} holds beside its status (#305, D6).
 * <p>
 * Its results are not here: each is its own file, compared by
 * {@link RunItemChangeComparator}. A tester reviewing a commit reads one row per
 * verdict that changed, under the case it is about, rather than one line saying a
 * run changed somehow.
 * <p>
 * Nothing here reverts. A verdict is a record of work, not an edit: putting it
 * back would say a case was never run.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class TestRunChangeComparator {

    /**
     * UC-SHARE-010, Rule-SHARE-046.
     * <p>
     * What changed in a run's own facts - the answers the tester gave when it was
     * created, and when it was executed - which its {@code .tr} holds beside its
     * status (#305, D6).
     * <p>
     * Through the same enums the Details popup and the reports read, so a change
     * reads under the heading they show it under and in the format they show it
     * in. Walked, not listed: a ninth question is compared by being declared
     * there and nowhere else.
     */
    static @NotNull List<FieldChange> compareFacts(final @NotNull TestRunMarker oldMarker, final @NotNull TestRunMarker newMarker) {
        final @NotNull List<FieldChange> changes = new ArrayList<>();

        for (final TestRunConfiguration field : TestRunConfiguration.values()) {
            addIfChanged(changes, field.getDisplayName(), field.valueIn(oldMarker), field.valueIn(newMarker));
        }

        for (final TestRunExecution field : TestRunExecution.values()) {
            addIfChanged(changes, field.getDisplayName(), field.valueIn(oldMarker), field.valueIn(newMarker));
        }

        return changes;
    }

    private static void addIfChanged(final @NotNull List<FieldChange> changes, final @NotNull String field, final @NotNull String oldValue, final @NotNull String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(new FieldChange(field, oldValue, newValue, ChangeType.CHANGE_MARKER));
        }
    }
}
