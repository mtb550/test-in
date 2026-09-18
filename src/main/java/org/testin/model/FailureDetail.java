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

/**
 * The six things on a run row that exist only to explain why a case is not
 * passing: what happened, the stacktrace behind it, the screenshots pasted with
 * it, how bad the bug is, and the GitHub issue it was reported as.
 * <p>
 * They are declared here as one list because two places need the same answer
 * about them and used to hold their own copies of it:
 * {@link TestRunItems#recordVerdict} clears them when a case passes, and the
 * verdict has to say what it would erase before it does. Another such field
 * added to the row is one constant here, and both places already know about it.
 */
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

    /**
     * The screenshots a tester pasted into the error box, kept beside the
     * stacktrace rather than inside it (#50).
     */
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

    /**
     * The GitHub issue the failure was reported as (#28). A passed case reports
     * no bug, so the link goes with the rest - and a later failure can be
     * reported again.
     */
    BUG_ISSUE_URL(
            Bundle.message("failure.detail.bug.issue.link"),
            item -> item.bugIssue().isPresent(),
            item -> item.setBugIssueUrl("")
    );

    /**
     * What it is called in the sentence that warns the tester it is about to go.
     * Lower case and with its article, because it is read inside a sentence
     * rather than as a heading.
     */
    private final @NotNull String label;

    private final @NotNull Predicate<TestRunItems> filled;

    private final @NotNull Consumer<TestRunItems> clear;

    /**
     * UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-220.
     * <p>
     * What happened, as against the bug: the three a failure the automation
     * reports clears before it writes its own. The bug - how bad it is, how soon
     * it must be fixed, the issue it was reported as - is still the same bug when
     * the same case fails again, so only a pass clears that.
     */
    public static final @NotNull List<FailureDetail> WHAT_HAPPENED = List.of(ACTUAL_RESULT, STACKTRACE, SCREENSHOTS);

    /**
     * UC-VIEW-PANEL-008, Rule-VIEW-PANEL-064.
     * <p>
     * Whether this row records a bug at all.
     * <p>
     * Three of them say a bug was found - how bad it is, how soon it must be
     * fixed, and the issue it was filed as - and the rest say what happened,
     * which a case can carry without anybody having filed anything. Asked here
     * because this enum already owns what "filled in" means for each of them,
     * and the Open Bugs tab must not come to its own answer beside the Details
     * tab's.
     * <p>
     * The filed issue counts on its own. Report Bug sets no severity and no
     * priority, and every automated failure is filed that way, so a case with
     * an issue open against it was listed as having no bugs (#312, A80).
     */
    public static boolean recordsABug(final @NotNull TestRunItems item) {
        return isTriaged(item) || BUG_ISSUE_URL.filled.test(item);
    }

    /**
     * UC-VIEW-PANEL-008, Rule-VIEW-PANEL-006.
     * <p>
     * Whether somebody said how bad this bug is or how soon it must be fixed. A
     * bug that was only filed has neither, and has no line to draw for them.
     */
    public static boolean isTriaged(final @NotNull TestRunItems item) {
        return BUG_SEVERITY.filled.test(item) || BUG_PRIORITY.filled.test(item);
    }

    /**
     * Everything this row holds that a pass would erase, in the tester's words,
     * and empty when a pass would erase nothing - which is the ordinary case.
     */
    public static @NotNull List<String> filledIn(final @NotNull TestRunItems item) {
        return filledIn(item, List.of(values()));
    }

    /**
     * The ones among these that this row holds, in the tester's words.
     */
    public static @NotNull List<String> filledIn(final @NotNull TestRunItems item, final @NotNull List<FailureDetail> details) {
        return details.stream()
                .filter(detail -> detail.filled.test(item))
                .map(FailureDetail::getLabel)
                .toList();
    }

    /**
     * Puts every one of them back to its empty value.
     */
    public static void clearAll(final @NotNull TestRunItems item) {
        clear(item, List.of(values()));
    }

    /**
     * Puts these back to their empty values.
     */
    public static void clear(final @NotNull TestRunItems item, final @NotNull List<FailureDetail> details) {
        details.forEach(detail -> detail.clear.accept(item));
    }
}
