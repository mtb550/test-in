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
import org.testin.util.Bundle;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter
@AllArgsConstructor
public enum FailureDetail {
    ACTUAL_RESULT(
            Bundle.message("failure.detail.actual.result"),
            item -> !item.getActualResult().isBlank(),
            item -> item.setActualResult("")
    ),

    STACKTRACE(
            Bundle.message("failure.detail.stacktrace"),
            item -> !item.getStacktrace().isBlank(),
            item -> item.setStacktrace("")
    ),

    SCREENSHOTS(
            Bundle.message("failure.detail.screenshots"),
            item -> !item.getScreenshots().isEmpty(),
            item -> item.setScreenshots(List.of())
    ),

    BUG_SEVERITY(
            Bundle.message("failure.detail.bug.severity"),
            item -> item.getBugSeverity() != BugSeverity.EMPTY,
            item -> item.setBugSeverity(BugSeverity.EMPTY)
    ),

    BUG_PRIORITY(
            Bundle.message("failure.detail.bug.priority"),
            item -> item.getBugPriority() != BugPriority.EMPTY,
            item -> item.setBugPriority(BugPriority.EMPTY)
    ),

    BUG_ISSUE_URL(
            Bundle.message("failure.detail.bug.issue.link"),
            item -> item.bugIssue().isPresent(),
            item -> item.setBugIssueUrl("")
    );

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-220
    public static final @NotNull List<FailureDetail> WHAT_HAPPENED = List.of(ACTUAL_RESULT, STACKTRACE, SCREENSHOTS);
    private final @NotNull String label;
    private final @NotNull Predicate<TestRunItems> filled;
    private final @NotNull Consumer<TestRunItems> clear;

    // UC-VIEW-PANEL-008, Rule-VIEW-PANEL-064
    public static boolean recordsABug(final @NotNull TestRunItems item) {
        return isTriaged(item) || BUG_ISSUE_URL.filled.test(item);
    }

    // UC-VIEW-PANEL-008, Rule-VIEW-PANEL-006
    public static boolean isTriaged(final @NotNull TestRunItems item) {
        return BUG_SEVERITY.filled.test(item) || BUG_PRIORITY.filled.test(item);
    }

    public static @NotNull List<String> filledIn(final @NotNull TestRunItems item) {
        return filledIn(item, List.of(values()));
    }

    public static @NotNull List<String> filledIn(final @NotNull TestRunItems item, final @NotNull List<FailureDetail> details) {
        return details.stream()
                .filter(detail -> detail.filled.test(item))
                .map(FailureDetail::getLabel)
                .toList();
    }

    public static void clearAll(final @NotNull TestRunItems item) {
        clear(item, List.of(values()));
    }

    public static void clear(final @NotNull TestRunItems item, final @NotNull List<FailureDetail> details) {
        details.forEach(detail -> detail.clear.accept(item));
    }
}
