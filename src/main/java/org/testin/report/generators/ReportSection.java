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

package org.testin.report.generators;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunItems;
import org.testin.model.TestRunSummary;
import org.testin.model.TestStatus;
import org.testin.util.Bundle;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.ToLongFunction;

enum ReportSection {
    FAILED(
            Bundle.message("report.section.failed.title"),
            Bundle.message("report.section.failed.description"),
            "F2685A",
            TestRunSummary::failed,
            true,
            TestStatus.FAILED),

    PASSED(
            Bundle.message("report.section.passed.title"),
            Bundle.message("report.section.passed.description"),
            "4FBF60",
            TestRunSummary::passed,
            false,
            TestStatus.PASSED),

    BLOCKED(
            Bundle.message("report.section.blocked.title"),
            Bundle.message("report.section.blocked.description"),
            "F5B940",
            TestRunSummary::blocked,
            false,
            TestStatus.BLOCKED),

    UNTESTED(
            Bundle.message("report.section.untested.title"),
            Bundle.message("report.section.untested.description"),
            "96A1B0",
            TestRunSummary::untested,
            false,
            TestStatus.PENDING,
            TestStatus.UNTESTED),

    REMOVED(
            Bundle.message("report.section.removed.title"),
            Bundle.message("report.section.removed.description"),
            "96A1B0",
            TestRunSummary::removed,
            false,
            TestStatus.REMOVED);

    @Getter
    private final @NotNull String title;
    private final @NotNull String descriptionFmt;
    @Getter
    private final @NotNull String hexColor;
    private final @NotNull ToLongFunction<TestRunSummary> count;
    @Getter
    private final boolean withFailureDetail;
    private final @NotNull Set<TestStatus> statuses;

    ReportSection(final @NotNull String title, final @NotNull String descriptionFmt, final @NotNull String hexColor, final @NotNull ToLongFunction<TestRunSummary> count, final boolean withFailureDetail, final @NotNull TestStatus... statuses) {
        this.title = title;
        this.descriptionFmt = descriptionFmt;
        this.hexColor = hexColor;
        this.count = count;
        this.withFailureDetail = withFailureDetail;
        this.statuses = EnumSet.copyOf(Arrays.asList(statuses));
    }

    private static double contrast(final @NotNull String one, final @NotNull String other) {
        final double first = luminance(one);
        final double second = luminance(other);

        return (Math.max(first, second) + 0.05) / (Math.min(first, second) + 0.05);
    }

    private static double luminance(final @NotNull String hex) {
        return 0.2126 * channel(hex, 0) + 0.7152 * channel(hex, 2) + 0.0722 * channel(hex, 4);
    }

    private static double channel(final @NotNull String hex, final int at) {
        final double raw = Integer.parseInt(hex.substring(at, at + 2), 16) / 255.0;

        return raw <= 0.03928 ? raw / 12.92 : Math.pow((raw + 0.055) / 1.055, 2.4);
    }

    public static @NotNull ReportSection of(final @NotNull TestRunItems item) {
        return Arrays.stream(values()).filter(section -> section.matches(item)).findFirst().orElseThrow();
    }

    public long count(final @NotNull TestRunSummary summary) {
        return count.applyAsLong(summary);
    }

    public @NotNull String description(final @NotNull String renderedCount) {
        return String.format(descriptionFmt, renderedCount);
    }

    public @NotNull String textHex() {
        return contrast(hexColor, "FFFFFF") >= contrast(hexColor, "14171A") ? "FFFFFF" : "14171A";
    }

    public boolean matches(final @NotNull TestRunItems item) {
        return statuses.contains(item.shownStatus());
    }
}
