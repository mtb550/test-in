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

package org.testin.report;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunSummary;
import org.testin.model.TestStatus;
import org.testin.util.Bundle;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * The headline figures a report opens with, and everything that differs between
 * them (#174).
 * <p>
 * Four generators printed this row and each wrote the list out: the same seven
 * labels, the same seven values off the same summary, and the same six colors -
 * declared twice over as {@code DeviceRgb} in the PDF and as hex strings in the
 * Word file, holding the identical six values. Two of them also decided how many
 * tiles there are with {@code hasRemoved() ? 7 : 6}, which is a fact about the
 * list stated beside the list rather than counted from it.
 * <p>
 * <b>They had already drifted.</b> The spreadsheet opened with Executed and no
 * Total Cases while the other three opened with Total Cases and no Executed, so
 * two reports of one run disagreed about what the run was - and nothing in the
 * code said whether that was meant. It was not: decided 2026-09-04, the
 * spreadsheet adopts this list like everything else.
 * <p>
 * Executed is not here and is not lost. It is Passed, Failed and Blocked added
 * up - which is Total minus Untested and Removed, because the total counts a
 * removed case and nobody ran one. The narrative sentence above the HTML tiles
 * still says it, and a figure a reader can work out does not need a tile of its
 * own in four formats.
 */
@Getter
@AllArgsConstructor
public enum ReportTile {

    TOTAL_CASES(Bundle.message("report.tile.total.cases"), "1F3864", "var(--heading)", summary -> String.valueOf(summary.total())),

    PASSED(TestStatus.PASSED.getLabel(), "2E7D32", "var(--verdict-passed)", summary -> String.valueOf(summary.passed())),

    FAILED(TestStatus.FAILED.getLabel(), "C0392B", "var(--verdict-failed)", summary -> String.valueOf(summary.failed())),

    BLOCKED(TestStatus.BLOCKED.getLabel(), "B8860B", "var(--verdict-blocked)", summary -> String.valueOf(summary.blocked())),

    UNTESTED(TestStatus.UNTESTED.getLabel(), "595959", "var(--verdict-untested)", summary -> String.valueOf(summary.untested())),

    /**
     * Only when the run has any.
     * <p>
     * The total counts removed cases, so without a tile of their own the figures
     * beside the total do not add up to it - which is why this is hidden rather
     * than printed as a zero, and why the row's width is counted rather than
     * assumed.
     */
    REMOVED(TestStatus.REMOVED.getLabel(), "595959", "var(--verdict-removed)", summary -> String.valueOf(summary.removed())) {
        @Override
        public boolean isShownFor(final @NotNull TestRunSummary summary) {
            return summary.hasRemoved();
        }
    },

    PASS_RATE(Bundle.message("report.tile.pass.rate"), "2E5496", "var(--heading)", summary -> summary.passRate() + "%");

    private final @NotNull String label;

    /**
     * The color as six hex digits, which is what the Word file wants directly and
     * what the PDF builds a {@code DeviceRgb} from. One value rather than the two
     * spellings of it those generators each kept.
     */
    private final @NotNull String hex;

    /**
     * The same color as the stylesheet's own token, so the HTML report follows the
     * theme it is rendered in rather than a hex that ignores it.
     */
    private final @NotNull String cssToken;

    private final @NotNull Function<TestRunSummary, String> value;

    /**
     * What this tile reads for a run.
     */
    public @NotNull String valueIn(final @NotNull TestRunSummary summary) {
        return value.apply(summary);
    }

    /**
     * Whether this run has anything to say under this heading. Every tile but
     * Removed always does.
     */
    public boolean isShownFor(final @NotNull TestRunSummary summary) {
        return true;
    }

    /**
     * Rule-REPORT-002.
     * <p>
     * The headline for one run, in order - and the count, which the two
     * generators that need a table width now take from {@code size()} rather than
     * from a ternary restating the rule above.
     */
    public static @NotNull List<ReportTile> shownFor(final @NotNull TestRunSummary summary) {
        return Arrays.stream(values()).filter(tile -> tile.isShownFor(summary)).toList();
    }
}
