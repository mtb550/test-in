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
import org.testin.model.ReportColor;
import org.testin.model.TestRunSummary;
import org.testin.model.TestStatus;
import org.testin.util.Bundle;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum ReportTile {
    TOTAL_TEST_CASES(
            Bundle.message("report.tile.total.cases"),
            ReportColor.HEADING.hex(),
            "var(--heading)",
            TestRunSummary::total,
            ""
    ),

    PASSED(
            TestStatus.PASSED.getLabel(),
            TestStatus.PASSED.getReportHex(),
            "var(--verdict-passed)",
            TestRunSummary::passed,
            ""
    ),

    FAILED(
            TestStatus.FAILED.getLabel(),
            TestStatus.FAILED.getReportHex(),
            "var(--verdict-failed)",
            TestRunSummary::failed,
            ""
    ),

    BLOCKED(
            TestStatus.BLOCKED.getLabel(),
            TestStatus.BLOCKED.getReportHex(),
            "var(--verdict-blocked)",
            TestRunSummary::blocked,
            ""
    ),

    UNTESTED(
            TestStatus.UNTESTED.getLabel(),
            TestStatus.UNTESTED.getReportHex(),
            "var(--verdict-untested)",
            TestRunSummary::untested,
            ""
    ),

    REMOVED(TestStatus.REMOVED.getLabel(), TestStatus.REMOVED.getReportHex(), "var(--verdict-removed)", TestRunSummary::removed, "") {
        @Override
        public boolean isShownFor(final @NotNull TestRunSummary summary) {
            return summary.hasRemoved();
        }
    },

    PASS_RATE(
            Bundle.message("report.tile.pass.rate"),
            ReportColor.ACCENT.hex(),
            "var(--heading)",
            TestRunSummary::passRate,
            "%"
    );

    private final @NotNull String label;

    private final @NotNull String hex;

    private final @NotNull String cssToken;

    private final @NotNull Function<TestRunSummary, Number> amount;

    private final @NotNull String unit;

    // Rule-REPORT-002
    public static @NotNull List<ReportTile> shownFor(final @NotNull TestRunSummary summary) {
        return Arrays.stream(values()).filter(tile -> tile.isShownFor(summary)).toList();
    }

    public @NotNull String valueIn(final @NotNull TestRunSummary summary) {
        return amountIn(summary) + unit;
    }

    public @NotNull Number amountIn(final @NotNull TestRunSummary summary) {
        return amount.apply(summary);
    }

    public boolean isShownFor(final @NotNull TestRunSummary summary) {
        return true;
    }
}
