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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.BugIssueUrl;
import org.testin.model.TestRunItems;
import org.testin.testrun.RunEditorAttributes;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Compares two revisions of one case's result, the way
 * {@link TestCaseChangeComparator} does for a test case (#305).
 * <p>
 * A result is what one tester recorded about one case: the verdict, what they
 * saw, how long it took, and what they filed about it. Each is a row, under the
 * caption the run editor's own column carries, so a change reads in the words
 * the tester gave it.
 * <p>
 * The screenshots are named rather than counted: the file names are what the
 * commit carries beside the result, and a tester reviewing a failure wants to
 * see that a picture arrived with it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class RunItemChangeComparator {

    // UC-SHARE-010, Rule-SHARE-046
    static @NotNull List<FieldChange> compare(final @NotNull TestRunItems oldItem, final @NotNull TestRunItems newItem) {
        final @NotNull List<FieldChange> changes = new ArrayList<>();

        addIfChanged(changes, RunEditorAttributes.RUN_STATUS.getName(), oldItem.getStatus().getLabel(), newItem.getStatus().getLabel());
        addIfChanged(changes, RunEditorAttributes.ACTUAL_RESULT.getName(), oldItem.getActualResult(), newItem.getActualResult());
        addIfChanged(changes, RunEditorAttributes.STACKTRACE.getName(), oldItem.getStacktrace(), newItem.getStacktrace());
        addIfChanged(changes, RunEditorAttributes.BUG_SEVERITY.getName(), oldItem.getBugSeverity().getLabel(), newItem.getBugSeverity().getLabel());
        addIfChanged(changes, RunEditorAttributes.BUG_PRIORITY.getName(), oldItem.getBugPriority().getLabel(), newItem.getBugPriority().getLabel());
        addIfChanged(changes, RunEditorAttributes.BUG_ISSUE.getName(), BugIssueUrl.reference(oldItem.getBugIssueUrl()), BugIssueUrl.reference(newItem.getBugIssueUrl()));
        addIfChanged(changes, RunEditorAttributes.EXECUTED_BY.getName(), oldItem.getExecutedBy(), newItem.getExecutedBy());
        addIfChanged(changes, RunEditorAttributes.EXECUTED_AT.getName(), Display.formatDate(oldItem.getExecutedAt()), Display.formatDate(newItem.getExecutedAt()));
        addIfChanged(changes, Bundle.message("git.change.screenshots"), String.join(", ", oldItem.getScreenshots()), String.join(", ", newItem.getScreenshots()));

        // A result that changed with nothing above different still changed - a
        // duration, a field this does not read - and it has to be selectable,
        // because the commit stages only what the review lists.
        if (changes.isEmpty()) {
            changes.add(new FieldChange(RunEditorAttributes.RUN_STATUS.getName(), "", Bundle.message("git.change.changed"), ChangeType.CHANGE_RUN_ITEM));
        }

        return changes;
    }

    /**
     * What a result says, in one line: the verdict, and what the tester saw when
     * it was not a pass.
     */
    static @NotNull String summary(final @NotNull TestRunItems item) {
        final @NotNull String said = item.getActualResult().isBlank() ? "" : " - " + item.getActualResult();
        return item.getStatus().getLabel() + said;
    }

    private static void addIfChanged(final @NotNull List<FieldChange> changes, final @NotNull String field, final @NotNull String oldValue, final @NotNull String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(new FieldChange(field, oldValue, newValue, ChangeType.CHANGE_RUN_ITEM));
        }
    }
}
